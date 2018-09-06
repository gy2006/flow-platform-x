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
import lombok.Setter;
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

    private final static int LoggingWaitSeconds = 30;

    private final static int ShutdownWaitSeconds = 30;

    private final static String LineSeparator = System.lineSeparator();

    @Getter
    private final Cmd cmd;

    @Getter
    private final ExecutedCmd result;

    @Getter
    private final ProcessBuilder pBuilder;

    @Getter
    private Process process;

    @Getter
    @Setter
    private LoggingListener loggingListener = new LoggingListener() {
    };

    @Getter
    @Setter
    private ProcessListener processListener = new ProcessListener() {
    };

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

    private final Queue<Log> loggingQueue = new ConcurrentLinkedQueue<>();

    private final String endTerm = String.format("=====EOF-%s=====", UUID.randomUUID());

    public ShellExecutor(Cmd cmd) {
        this.cmd = cmd;
        this.result = new ExecutedCmd(cmd.getId());

        // init process builder
        this.pBuilder = new ProcessBuilder(LinuxBash).directory(getWorkDir(cmd).toFile());

        // init inputs env
        this.pBuilder.environment().putAll(cmd.getInputs().toStringMap());

        // support exit value
        this.cmd.getScripts().add(0, "set -e");
    }

    public void run() {
        try {
            result.setStartAt(new Date());
            result.setStatus(Status.RUNNING);

            process = pBuilder.start();
            result.setProcessId(getPid(process));
            processListener.onStarted(result);

            // thread to send cmd list to bash
            executor.execute(createCmdListExec(process.getOutputStream(), cmd.getScripts()));

            // thread to read stdout and stderr stream and put log to logging queue
            executor.execute(createStdStreamReader(Log.Type.STDOUT, process.getInputStream()));
            executor.execute(createStdStreamReader(Log.Type.STDERR, process.getErrorStream()));

            // thread to make consume logging queue
            executor.execute(createCmdLoggingReader());

            // wait for max process timeout
            if (process.waitFor(cmd.getTimeout(), TimeUnit.SECONDS)) {
                result.setCode(process.exitValue());
                result.setStatus(Status.SUCCESS);
            } else {
                result.setCode(ExecutedCmd.CODE_TIMEOUT);
                result.setStatus(Status.TIMEOUT);
            }

            log.trace("====== Process executed : {} ======", result.getCode());

            // wait for log thread with max 30 seconds to continue upload log
            logThreadCountDown.await(LoggingWaitSeconds, TimeUnit.SECONDS);
            executor.shutdown();

            // try to shutdown all threads with max 30 seconds waiting time
            if (!executor.awaitTermination(ShutdownWaitSeconds, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }

            result.setFinishAt(new Date());
            processListener.onExecuted(result);
            log.trace("====== Logging executed ======");

        } catch (InterruptedException e) {
            result.setStatus(Status.KILLED);
            result.setError(e.getMessage());
            processListener.onException(e);
            log.trace("====== Interrupted ======");
        } catch (Throwable e) {
            result.setStatus(Status.EXCEPTION);
            result.setError(e.getMessage());
            processListener.onException(e);
            log.warn(e.getMessage());
        } finally {
            result.setFinishAt(new Date());
            log.trace("====== Process Done ======");
        }
    }

    public void destroy() {
        if (Objects.isNull(process)) {
            return;
        }

        process.destroy();
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
            try {
                while (true) {
                    if (stdThreadCountDown.getCount() == 0 && loggingQueue.size() == 0) {
                        break;
                    }

                    Log log = loggingQueue.poll();
                    if (log == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                    } else {
                        loggingListener.onLogging(log);
                    }
                }
            } finally {
                loggingListener.onFinish();
                logThreadCountDown.countDown();
                log.trace(" ===== Logging Reader Thread Finish =====");
            }
        };
    }

    private Runnable createStdStreamReader(final Log.Type type, final InputStream is) {
        return () -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is), BufferSize)) {
                String line;
                Integer count = 0;
                while ((line = reader.readLine()) != null) {
                    if (Objects.equals(line, endTerm)) {
                        readEnv(reader);
                        break;
                    }
                    count += 1;
                    loggingQueue.add(Log.of(type, line, count));
                }
            } catch (IOException ignore) {

            } finally {
                stdThreadCountDown.countDown();
                log.trace(" ===== {} Stream Reader Thread Finish =====", type);
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
