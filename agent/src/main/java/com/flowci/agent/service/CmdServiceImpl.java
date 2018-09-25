/*
 * Copyright 2018 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.agent.service;

import com.flowci.agent.config.AgentProperties;
import com.flowci.agent.dao.ExecutedCmdDao;
import com.flowci.agent.dao.ReceivedCmdDao;
import com.flowci.agent.domain.AgentExecutedCmd;
import com.flowci.agent.domain.AgentReceivedCmd;
import com.flowci.agent.event.CmdCompleteEvent;
import com.flowci.agent.event.CmdReceivedEvent;
import com.flowci.agent.executor.Log;
import com.flowci.agent.executor.LoggingListener;
import com.flowci.agent.executor.ProcessListener;
import com.flowci.agent.executor.ShellExecutor;
import com.flowci.domain.Cmd;
import com.flowci.domain.CmdType;
import com.flowci.domain.ExecutedCmd;
import com.flowci.exception.NotFoundException;
import com.google.common.collect.ImmutableList;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class CmdServiceImpl implements CmdService {

    private static final Sort SortByReceivedAt = Sort.by(Direction.DESC, "receivedAt");

    private static final Sort SortByStartAt = Sort.by(Direction.DESC, "startAt");

    private static final Page<String> LogNotFound = new PageImpl<>(ImmutableList.of("Log does not existed on agent"));

    @Autowired
    private AgentProperties agentProperties;

    @Autowired
    private Queue callbackQueue;

    @Autowired
    private RabbitTemplate queueTemplate;

    @Autowired
    private ReceivedCmdDao receivedCmdDao;

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Autowired
    private ApplicationContext context;

    private ThreadPoolTaskExecutor cmdThreadPool = createExecutor();

    private final Object lock = new Object();

    private Cmd current;

    @Override
    public Cmd get(String id) {
        Optional<AgentReceivedCmd> optional = receivedCmdDao.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("Cmd {0} is not found", id);
    }

    @Override
    public Page<String> getLogs(String id, Pageable pageable) {
        Path logPath = getCmdLogPath(id);

        if (!Files.exists(logPath)) {
            return LogNotFound;
        }

        ExecutedCmd executedCmd = getExecutedCmd(id);
        int i = pageable.getPageNumber() * pageable.getPageSize();

        try (Stream<String> lines = Files.lines(logPath)) {
            List<String> logs = lines.skip(i)
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());

            return new PageImpl<>(logs, pageable, executedCmd.getLogSize());
        } catch (IOException e) {
            return LogNotFound;
        }
    }

    @Override
    public Page<AgentReceivedCmd> listReceivedCmd(int page, int size) {
        return receivedCmdDao.findAll(PageRequest.of(page, size, SortByReceivedAt));
    }

    @Override
    public Page<AgentExecutedCmd> listExecutedCmd(int page, int size) {
        return executedCmdDao.findAll(PageRequest.of(page, size, SortByStartAt));
    }

    @Override
    public void execute(Cmd cmd) {
        if (cmd.getType() == CmdType.SHELL) {
            if (hasCmdRunning()) {
                log.debug("Cannot start cmd since {} is running", current);
                return;
            }

            setCurrent(save(cmd));
            context.publishEvent(new CmdReceivedEvent(this, current));

            if (!current.hasWorkDir()) {
                current.setWorkDir(agentProperties.getWorkspace());
            }

            cmdThreadPool.execute(() -> {
                ShellExecutor cmdExecutor = new ShellExecutor(current);
                cmdExecutor.setProcessListener(new CmdProcessListener(cmd));
                cmdExecutor.setLoggingListener(new CmdLoggingListener(cmd));
                cmdExecutor.run();
                onAfterExecute(cmdExecutor.getResult());
            });

            return;
        }

        if (cmd.getType() == CmdType.KILL) {
            cmdThreadPool.setWaitForTasksToCompleteOnShutdown(false);
            cmdThreadPool.shutdown();
            cmdThreadPool.initialize();
            return;
        }

        if (cmd.getType() == CmdType.CLOSE) {
            int exitCode = SpringApplication.exit(context);
            log.info("Agent closed");
            System.exit(exitCode);
        }
    }

    @Override
    public void onCmdReceived(Cmd received) {
        log.debug("Cmd received: {}", received);
        execute(received);
    }

    private void onAfterExecute(ExecutedCmd executed) {
        AgentExecutedCmd agentExecutedCmd = new AgentExecutedCmd();
        BeanUtils.copyProperties(executed, agentExecutedCmd);
        executedCmdDao.save(agentExecutedCmd);

        queueTemplate.convertAndSend(callbackQueue.getName(), executed);
        setCurrent(null);
        context.publishEvent(new CmdCompleteEvent(this, current, executed));
    }

    private ExecutedCmd getExecutedCmd(String id) {
        Optional<AgentExecutedCmd> optional = executedCmdDao.findById(id);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Executed Cmd {0} is not found", id);
    }

    private Cmd getCurrent() {
        synchronized (lock) {
            return current;
        }
    }

    private void setCurrent(Cmd cmd) {
        synchronized (lock) {
            current = cmd;
        }
    }

    private boolean hasCmdRunning() {
        return getCurrent() != null && cmdThreadPool.getActiveCount() > 0;
    }

    private Cmd save(Cmd cmd) {
        AgentReceivedCmd agentCmd = new AgentReceivedCmd();
        BeanUtils.copyProperties(cmd, agentCmd);

        agentCmd.setReceivedAt(new Date());
        return receivedCmdDao.save(agentCmd);
    }

    private ThreadPoolTaskExecutor createExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(1);
        executor.setCorePoolSize(1);
        executor.setQueueCapacity(0);
        executor.setDaemon(true);
        executor.setThreadNamePrefix("cmd-exec-thread-");
        executor.initialize();
        return executor;
    }

    private Path getCmdLogPath(String id) {
        return Paths.get(agentProperties.getLoggingDir(), id + ".log");
    }

    private class CmdLoggingListener implements LoggingListener {

        private final Cmd cmd;

        private BufferedWriter writer;

        public CmdLoggingListener(Cmd cmd) {
            this.cmd = cmd;
            initWriter();
        }

        @Override
        public void onLogging(Log item) {
            log.debug("Log Received : {}", item);
            try {
                writer.write(item.getContent());
                writer.newLine();
            } catch (IOException ignore) {
            }
        }

        @Override
        public void onFinish(long size) {
            if (Objects.isNull(writer)) {
                return;
            }

            try {
                writer.close();
            } catch (IOException ignore) {
            }
        }

        private void initWriter() {
            try {
                Path path = getCmdLogPath(cmd.getId());
                writer = Files.newBufferedWriter(path);
            } catch (IOException ignore) {

            }
        }
    }

    private class CmdProcessListener implements ProcessListener {

        private ExecutedCmd executed;

        private final Cmd cmd;

        public CmdProcessListener(Cmd cmd) {
            this.cmd = cmd;
        }

        @Override
        public void onStarted(ExecutedCmd executed) {
            this.executed = executed;
            log.debug("Cmd Started : {}", executed);
        }

        @Override
        public void onExecuted(ExecutedCmd executed) {
            this.executed = executed;
            log.debug("Cmd Executed : {}", executed);
        }

        @Override
        public void onException(Throwable e) {
            if (executed != null) {
                executed.setError(e.getMessage());
            }
            log.debug("Cmd Exception : {}", e);
        }
    }
}
