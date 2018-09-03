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

package com.flowci.agent.test.executor;

import com.flowci.agent.executor.CmdExecutor;
import com.flowci.agent.executor.Log;
import com.flowci.agent.executor.LoggingListener;
import com.flowci.domain.Cmd;
import com.flowci.domain.ExecutedCmd;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class ExecutorTest {

    private LoggingListener logListener = new LoggingListener() {

        @Override
        public void onLogging(Log log) {
            System.out.println(log);
        }
    };

    @Test
    public void should_execute_command_with_correct_event() throws Throwable {
        ClassLoader loader = this.getClass().getClassLoader();
        String path = loader.getResource("test.sh").getFile();
        Runtime.getRuntime().exec("chmod +x " + path);

        Cmd cmd = new Cmd("cmd.id");
        cmd.setScripts(Lists.newArrayList(String.format("source %s", path)));
        cmd.setEnvFilters(Sets.newHashSet("CMD_RUNNER_TEST_", "OUTPUT_2"));

        // run test.sh and export var start with CMD_RUNNER_TEST_ and OUTPUT_2
        CmdExecutor executor = new CmdExecutor(cmd);
        executor.setLoggingListener(logListener);
        executor.run();

        ExecutedCmd result = executor.getResult();
        Assert.assertEquals(0, result.getCode().intValue());
        Assert.assertNotNull(result.getProcessId());
        Assert.assertNotNull(result.getDuration());
        Assert.assertNotNull(result.getStartAt());
        Assert.assertNotNull(result.getFinishAt());

        Map<String, String> output = result.getOutput().toStringMap();
        Assert.assertEquals(2, output.size());
        Assert.assertEquals("test1", output.get("CMD_RUNNER_TEST_1"));
        Assert.assertEquals("test2", output.get("OUTPUT_2"));
    }

    @Test
    public void should_not_export_output_when_cmd_got_error() throws Throwable {
        ClassLoader loader = this.getClass().getClassLoader();
        String path = loader.getResource("test_with_cmd_err.sh").getFile();
        Runtime.getRuntime().exec("chmod +x " + path);


        Cmd cmd = new Cmd("cmd.id");
        cmd.setScripts(Lists.newArrayList(String.format("source %s", path)));
        cmd.setEnvFilters(Sets.newHashSet("CMD_RUNNER_TEST"));

        CmdExecutor executor = new CmdExecutor(cmd);
        executor.run();

        ExecutedCmd result = executor.getResult();
        Assert.assertEquals(0, result.getOutput().size());
        Assert.assertNotEquals(0, result.getCode().intValue());
    }

}
