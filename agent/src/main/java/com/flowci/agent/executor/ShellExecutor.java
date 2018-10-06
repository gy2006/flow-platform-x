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

package com.flowci.agent.executor;

import com.flowci.domain.Cmd;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.ExecutedCmd.Status;
import com.flowci.domain.LogItem;
import com.flowci.util.UnixHelper;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Cmd executor to run cmd
 *
 * @author yang
 */
@Log4j2
public class ShellExecutor {

    private final static String LinuxBash = "/bin/bash";

    private final static Path DefaultWorkDir =
        Paths.get(System.getProperty("user.home", System.getProperty("user.dir")));

    // 1 mb buffer for std reader
    private final static int BufferSize = 1024 * 1024 * 1;

    private final static int LoggingWaitSeconds = 5;

    private final static int ShutdownWaitSeconds = 0;

    private final static String LineSeparator = System.lineSeparator();

    @Getter
    private final Cmd cmd;

    @Getter
    private final ExecutedCmd result;

    @Getter
    private final ProcessBuilder pBuilder;

    @Getter
    private final List<LoggingListener> loggingListeners = new LinkedList<>();

    @Getter
    private final List<ProcessListener> processListeners = new LinkedList<>();

    @Getter
    private Process process;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setDaemon(true);
            return t;
        }
    );

    private final CountDownLatch stdThreadCountDown = new CountDownLatch(2);

    private final CountDownLatch logThreadCountDown = new CountDownLatch(1);

    private final Queue<LogItem> loggingQueue = new ConcurrentLinkedQueue<>();

    private final String endTerm = String.format("=====EOF-%s=====", UUID.randomUUID());

    public ShellExecutor(Cmd cmd) {
        this.cmd = cmd;
        this.result = new ExecutedCmd(cmd.getId());

        // init process builder
        this.pBuilder = new ProcessBuilder(LinuxBash).directory(getWorkDir(cmd).toFile());

        // init inputs env
        this.pBuilder.environment().putAll(cmd.getInputs());

        // support exit value
        this.cmd.getScripts().add(0, "set -e");
    }

    public void run() {
        try {
            result.setStartAt(new Date());
            result.setStatus(Status.RUNNING);

            process = pBuilder.start();
            result.setProcessId(getPid(process));

            for (ProcessListener processListener : processListeners) {
                processListener.onStarted(result);
            }

            // thread to send cmd list to bash
            executor.execute(createCmdListExec(process.getOutputStream(), cmd.getScripts()));

            // thread to read stdout and stderr stream and put log to logging queue
            executor.execute(createStdStreamReader(LogItem.Type.STDOUT, process.getInputStream()));
            executor.execute(createStdStreamReader(LogItem.Type.STDERR, process.getErrorStream()));

            // thread to make consume logging queue
            executor.execute(createCmdLoggingReader());

            // wait for max process timeout
            if (process.waitFor(cmd.getTimeout(), TimeUnit.SECONDS)) {
                result.setCode(process.exitValue());
            } else {
                result.setCode(ExecutedCmd.CODE_TIMEOUT);
            }

            result.setStatusByCode();
            result.setFinishAt(new Date());

            for (ProcessListener processListener : processListeners) {
                processListener.onExecuted(result);
            }

            log.debug("====== Process executed : {} ======", result.getCode());

            logThreadCountDown.await(LoggingWaitSeconds, TimeUnit.SECONDS);
            log.debug("====== Logging executed ======");

            executor.shutdown();
            if (!executor.awaitTermination(ShutdownWaitSeconds, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            log.debug("====== Executor Shutdown ======");

        } catch (InterruptedException e) {
            result.setStatus(Status.KILLED);
            result.setError(e.getMessage());

            for (ProcessListener processListener : processListeners) {
                processListener.onException(e);
            }

            log.debug("====== Interrupted ======");
        } catch (Throwable e) {
            result.setStatus(Status.EXCEPTION);
            result.setError(e.getMessage());

            for (ProcessListener processListener : processListeners) {
                processListener.onException(e);
            }

            log.warn(e.getMessage());
        } finally {
            result.setFinishAt(new Date());
            log.debug("====== Process Done ======");
        }
    }

    /**
     * Get process id
     */
    private int getPid(Process process) {
        try {
            Class<?> cProcessImpl = process.getClass();
            Field fPid = cProcessImpl.getDeclaredField("pid");
            if (!fPid.isAccessible()) {
                fPid.setAccessible(true);
            }
            return fPid.getInt(process);
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Make runnable to exec each cmd
     */
    private Runnable createCmdListExec(final OutputStream outputStream, final List<String> cmdList) {

        return () -> {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                for (String cmd : cmdList) {
                    writer.write(cmd + LineSeparator);
                    writer.flush();
                }

                // find env and set to result output if output filter is not null or empty
                if (!cmd.getEnvFilters().isEmpty()) {
                    writer.write(String.format("echo %s" + LineSeparator, endTerm));
                    writer.write("env" + LineSeparator);
                    writer.flush();
                }

            } catch (IOException e) {
                log.warn("Exception on write cmd: " + e.getMessage());
            }
        };
    }

    private Runnable createCmdLoggingReader() {
        return () -> {
            long lineNum = 0;

            try {
                while (true) {
                    if (stdThreadCountDown.getCount() == 0 && loggingQueue.size() == 0) {
                        break;
                    }

                    LogItem log = loggingQueue.poll();

                    if (Objects.isNull(log)) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {

                        }

                        continue;
                    }

                    log.setNumber(++lineNum);
                    result.setLogSize(lineNum);

                    for (LoggingListener loggingListener : loggingListeners) {
                        loggingListener.onLogging(log);
                    }

                }
            } finally {
                for (LoggingListener loggingListener : loggingListeners) {
                    loggingListener.onFinish(lineNum);
                }

                logThreadCountDown.countDown();
                log.trace(" ===== Logging Reader Thread Finish =====");
            }
        };
    }

    private Runnable createStdStreamReader(final LogItem.Type type, final InputStream is) {
        return () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is), BufferSize)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (Objects.equals(line, endTerm)) {
                        readEnv(reader);
                        break;
                    }
                    loggingQueue.add(LogItem.of(type, line));
                }
            } catch (IOException ignore) {

            } finally {
                stdThreadCountDown.countDown();
                log.debug(" ===== {} Stream Reader Thread Finish =====", type);
            }
        };
    }

    /**
     * Start when find log match 'endTerm', and load all env,
     * put env item which match 'start with filter' to CmdResult.output map
     */
    private void readEnv(final BufferedReader reader) throws IOException {
        String line;
        String currentKey = null;
        StringBuilder value = null;

        while ((line = reader.readLine()) != null) {
            int index = line.indexOf('=');

            // reset value builder and current key
            if (index != -1 && !isMatchEnvFilter(line, cmd.getEnvFilters())) {
                if (value != null && currentKey != null) {
                    result.getOutput().putString(currentKey, value.toString());
                }

                currentKey = null;
                value = null;
                continue;
            }

            if (isMatchEnvFilter(line, cmd.getEnvFilters())) {

                // put previous env to output and reset
                if (value != null && currentKey != null) {
                    result.getOutput().putString(currentKey, value.toString());
                    value = null;
                    currentKey = null;
                }

                value = new StringBuilder();
                currentKey = line.substring(0, index);
                value.append(line.substring(index + 1));
                continue;
            }

            if (index == -1 && value != null) {
                value.append(LineSeparator + line);
            }
        }
    }

    private boolean isMatchEnvFilter(final String line, final Set<String> filters) {
        for (String filter : filters) {
            if (line.startsWith(filter)) {
                return true;
            }
        }
        return false;
    }

    private Path getWorkDir(Cmd cmd) {
        if (Objects.isNull(cmd.getWorkDir())) {
            return DefaultWorkDir;
        }

        Path dir = UnixHelper.replacePathWithEnv(cmd.getWorkDir());

        if (Files.exists(dir)) {
            return dir;
        }

        try {
            return Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to create working dir: " + dir);
        }
    }
}
