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

package com.flowci.agent.test.manager;

import com.flowci.agent.domain.AgentReceivedCmd;
import com.flowci.agent.event.CmdCompleteEvent;
import com.flowci.agent.event.CmdReceivedEvent;
import com.flowci.agent.service.CmdService;
import com.flowci.agent.test.SpringScenario;
import com.flowci.domain.Agent;
import com.flowci.domain.Cmd;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.ExecutedCmd.Status;
import com.flowci.domain.ObjectWrapper;
import com.flowci.domain.Settings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

/**
 * @author yang
 */
public class CmdManagerTest extends SpringScenario {

    @Autowired
    private RabbitTemplate queueTemplate;

    @Autowired
    private Settings agentSettings;

    @Autowired
    private CmdService cmdManager;

    @Test
    public void should_receive_cmd_from_server() throws InterruptedException {
        // init:
        Cmd cmd = new Cmd("cmd.id.1");
        cmd.setTimeout(10L);
        cmd.setScripts(Lists.newArrayList("echo hello"));
        cmd.setWorkDir("${HOME}");
        cmd.setEnvFilters(Sets.newHashSet("FLOW_"));
        cmd.getInputs().putString("HELLO_WORLD", "hello");

        // when:
        CountDownLatch counter = new CountDownLatch(1);
        ObjectWrapper<Cmd> cmdWrapper = new ObjectWrapper<>();

        applicationEventMulticaster.addApplicationListener((ApplicationListener<CmdReceivedEvent>) event -> {
            cmdWrapper.setValue(event.getCmd());
            counter.countDown();
        });

        Agent agent = agentSettings.getAgent();
        queueTemplate.convertAndSend(agent.getQueueName(), cmd);

        // then:
        Assert.assertTrue(counter.await(10, TimeUnit.SECONDS));

        Cmd received = cmdWrapper.getValue();
        Assert.assertNotNull(received);
        Assert.assertTrue(received instanceof AgentReceivedCmd);
        Assert.assertEquals(cmd, received);
        Assert.assertEquals(cmd, cmdManager.get(cmd.getId()));

        Assert.assertEquals("${HOME}", received.getWorkDir());
        Assert.assertEquals(10L, received.getTimeout().longValue());
        Assert.assertEquals("hello", received.getInputs().getString("HELLO_WORLD"));
        Assert.assertTrue(received.getEnvFilters().contains("FLOW_"));
        Assert.assertTrue(received.getScripts().contains("echo hello"));
    }

    @Test
    public void should_execute_cmd_with_success_status() throws InterruptedException {
        // init: create cmd to run
        Cmd cmd = new Cmd(UUID.randomUUID().toString());
        cmd.setScripts(Lists.newArrayList(""
            + "echo \"test shell\"\n"
            + "export CMD_RUNNER_TEST_1=test1\n"
            + "export OUTPUT_2=test2"));
        cmd.setEnvFilters(Sets.newHashSet("CMD_RUNNER"));
        cmd.setTimeout(10L);

        // when:
        CountDownLatch counter = new CountDownLatch(1);
        ObjectWrapper<ExecutedCmd> executedWrapper = new ObjectWrapper<>();

        applicationEventMulticaster.addApplicationListener((ApplicationListener<CmdCompleteEvent>) event -> {
            executedWrapper.setValue(event.getExecuted());
            counter.countDown();
        });

        cmdManager.onCmdReceived(cmd);

        // then:
        counter.await(10, TimeUnit.SECONDS);
        ExecutedCmd executed = executedWrapper.getValue();

        Assert.assertNotNull(executed);
        Assert.assertEquals(cmd.getId(), executed.getId());
        Assert.assertEquals(0, executed.getCode().intValue());
        Assert.assertEquals(Status.SUCCESS, executed.getStatus());
        Assert.assertEquals("test1", executed.getOutput().getString("CMD_RUNNER_TEST_1"));
    }
}
