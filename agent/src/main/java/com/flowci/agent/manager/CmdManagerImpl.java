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

package com.flowci.agent.manager;

import com.flowci.agent.dao.AgentCmdDao;
import com.flowci.agent.domain.AgentCmd;
import com.flowci.agent.event.CmdCompleteEvent;
import com.flowci.agent.event.CmdReceivedEvent;
import com.flowci.agent.executor.CmdExecutor;
import com.flowci.agent.executor.Log;
import com.flowci.agent.executor.LoggingListener;
import com.flowci.agent.executor.ProcessListener;
import com.flowci.domain.Cmd;
import com.flowci.domain.ExecutedCmd;
import com.flowci.exception.NotFoundException;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class CmdManagerImpl implements CmdManager {

    @Autowired
    private AgentCmdDao agentCmdDao;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    private ThreadPoolTaskExecutor cmdThreadPool = createExecutor();

    @Override
    public Cmd get(String id) {
        Optional<AgentCmd> optional = agentCmdDao.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("Cmd {0} is not found", id);
    }

    @Override
    public void onCmdReceived(Cmd received) {
        log.debug("Cmd received: {}", received);

        Cmd cmd = save(received);
        applicationEventPublisher.publishEvent(new CmdReceivedEvent(this, cmd));

        cmdThreadPool.execute(() -> {
            CmdExecutor cmdExecutor = new CmdExecutor(cmd);
            cmdExecutor.setProcessListener(new CmdProcessListener());
            cmdExecutor.setLoggingListener(new CmdLoggingListener());
            cmdExecutor.run();
            applicationEventPublisher.publishEvent(new CmdCompleteEvent(this, cmd, cmdExecutor.getResult()));
        });
    }

    private Cmd save(Cmd cmd) {
        AgentCmd agentCmd = new AgentCmd();
        BeanUtils.copyProperties(cmd, agentCmd);
        return agentCmdDao.save(agentCmd);
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

    private class CmdLoggingListener implements LoggingListener {

        @Override
        public void onLogging(Log item) {
            log.debug("Log Received : {}", item);
        }

        @Override
        public void onFinish() {

        }
    }

    private class CmdProcessListener implements ProcessListener {

        @Getter
        private ExecutedCmd executed;

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
