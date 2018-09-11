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

package com.flowci.agent.test.service;

import com.flowci.agent.dao.ExecutedCmdDao;
import com.flowci.agent.dao.ReceivedCmdDao;
import com.flowci.agent.domain.AgentExecutedCmd;
import com.flowci.agent.domain.AgentReceivedCmd;
import com.flowci.agent.event.CmdCompleteEvent;
import com.flowci.agent.event.CmdReceivedEvent;
import com.flowci.agent.service.CmdService;
import com.flowci.agent.test.SpringScenario;
import com.flowci.domain.Agent;
import com.flowci.domain.Cmd;
import com.flowci.domain.CmdType;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.ExecutedCmd.Status;
import com.flowci.domain.ObjectWrapper;
import com.flowci.domain.Settings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Page;

/**
 * @author yang
 */
public class CmdServiceTest extends SpringScenario {

    @Autowired
    private RabbitTemplate queueTemplate;

    @Autowired
    private Settings agentSettings;

    @Autowired
    private CmdService cmdService;

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Autowired
    private ReceivedCmdDao receivedCmdDao;

    @Before
    public void clean() {
        receivedCmdDao.deleteAll();
        executedCmdDao.deleteAll();
    }

    @Test
    public void should_list_received_cmd_by_date() {
        // init:
        AgentReceivedCmd first = new AgentReceivedCmd();
        first.setId("1");
        first.setReceivedAt(new Date());
        first.setType(CmdType.SHELL);
        receivedCmdDao.save(first);

        AgentReceivedCmd second = new AgentReceivedCmd();
        second.setId("2");
        second.setReceivedAt(Date.from(Instant.now().plus(10, ChronoUnit.SECONDS)));
        second.setType(CmdType.SHELL);
        receivedCmdDao.save(second);

        // when:
        Page<AgentReceivedCmd> page = cmdService.listReceivedCmd(0, 10);

        // then:
        Assert.assertNotNull(page);
        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertEquals("2", page.getContent().get(0).getId());
        Assert.assertEquals("1", page.getContent().get(1).getId());
    }

    @Test
    public void should_list_executed_cmd_by_start_at() {
        // init:
        AgentExecutedCmd first = new AgentExecutedCmd();
        first.setId("1");
        first.setStatus(Status.SUCCESS);
        first.setStartAt(new Date());
        executedCmdDao.save(first);

        AgentExecutedCmd second = new AgentExecutedCmd();
        second.setId("2");
        second.setStatus(Status.SUCCESS);
        second.setStartAt(Date.from(Instant.now().plus(10, ChronoUnit.SECONDS)));
        executedCmdDao.save(second);

        // when:
        Page<AgentExecutedCmd> page = cmdService.listExecutedCmd(0, 10);

        // then:
        Assert.assertNotNull(page);
        Assert.assertEquals(2, page.getTotalElements());
        Assert.assertEquals("2", page.getContent().get(0).getId());
        Assert.assertEquals("1", page.getContent().get(1).getId());
    }

    @Test
    public void should_receive_cmd_from_server() throws InterruptedException {
        // init:
        Cmd cmd = new Cmd("cmd.id.1", CmdType.SHELL);
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
        Assert.assertEquals(cmd, cmdService.get(cmd.getId()));

        Assert.assertEquals("${HOME}", received.getWorkDir());
        Assert.assertEquals(10L, received.getTimeout().longValue());
        Assert.assertEquals("hello", received.getInputs().getString("HELLO_WORLD"));
        Assert.assertTrue(received.getEnvFilters().contains("FLOW_"));
        Assert.assertTrue(received.getScripts().contains("echo hello"));
    }

    @Test
    public void should_execute_cmd_with_success_status() throws InterruptedException {
        // init: create cmd to run
        Cmd cmd = new Cmd(UUID.randomUUID().toString(), CmdType.SHELL);
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

        cmdService.onCmdReceived(cmd);

        // then:
        counter.await(10, TimeUnit.SECONDS);
        ExecutedCmd executed = executedWrapper.getValue();

        Assert.assertNotNull(executed);
        Assert.assertEquals(cmd.getId(), executed.getId());
        Assert.assertEquals(0, executed.getCode().intValue());
        Assert.assertEquals(Status.SUCCESS, executed.getStatus());
        Assert.assertEquals("test1", executed.getOutput().getString("CMD_RUNNER_TEST_1"));

        Assert.assertNotNull(executed.getStartAt());
        Assert.assertNotNull(executed.getFinishAt());
    }

    @Test
    public void should_execute_cmd_and_kill() throws Throwable {
        // init:
        Cmd cmd = new Cmd(UUID.randomUUID().toString(), CmdType.SHELL);
        cmd.setScripts(Lists.newArrayList("echo '--- start ---' && sleep 9999 && echo '--- end ---'"));
        cmd.setEnvFilters(Sets.newHashSet("CMD_RUNNER"));
        cmd.setTimeout(10L);

        // when: execute and kill
        CountDownLatch counter = new CountDownLatch(1);
        ObjectWrapper<ExecutedCmd> wrapper = new ObjectWrapper<>();
        applicationEventMulticaster.addApplicationListener((ApplicationListener<CmdCompleteEvent>) event -> {
            wrapper.setValue(event.getExecuted());
            counter.countDown();
        });

        cmdService.execute(cmd);
        Thread.sleep(1000);
        cmdService.execute(new Cmd(UUID.randomUUID().toString(), CmdType.KILL));

        // then:
        counter.await(10, TimeUnit.SECONDS);

        ExecutedCmd executed = wrapper.getValue();
        Assert.assertNotNull(executed);
        Assert.assertEquals(Status.KILLED, executed.getStatus());
        Assert.assertNotNull(executed.getFinishAt());
    }

    @Test
    public void should_execute_cmd_and_timeout() throws Throwable {
        Cmd cmd = new Cmd(UUID.randomUUID().toString(), CmdType.SHELL);
        cmd.setScripts(Lists.newArrayList("echo '--- start ---' && sleep 9999 && echo '--- end ---'"));
        cmd.setTimeout(2L);

        CountDownLatch counter = new CountDownLatch(1);
        ObjectWrapper<ExecutedCmd> wrapper = new ObjectWrapper<>();
        applicationEventMulticaster.addApplicationListener((ApplicationListener<CmdCompleteEvent>) event -> {
            wrapper.setValue(event.getExecuted());
            counter.countDown();
        });

        cmdService.execute(cmd);
        Assert.assertTrue(counter.await(10, TimeUnit.SECONDS));

        ExecutedCmd executed = wrapper.getValue();
        Assert.assertNotNull(executed);
        Assert.assertEquals(Status.TIMEOUT, executed.getStatus());
        Assert.assertNotNull(executed.getFinishAt());
    }
}
