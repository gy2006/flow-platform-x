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

package com.flowci.core.test.job;

import com.flowci.core.agent.event.CmdSentEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.flow.FlowService;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.job.dao.ExecutedCmdDao;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Status;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.service.JobService;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.domain.Agent;
import com.flowci.domain.Cmd;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.ObjectWrapper;
import com.flowci.domain.VariableMap;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import com.flowci.util.StringHelper;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

/**
 * @author yang
 */
@FixMethodOrder(MethodSorters.JVM)
public class JobServiceTest extends ZookeeperScenario {

    @Autowired
    private JobDao jobDao;

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Autowired
    private FlowService flowService;

    @Autowired
    private JobService jobService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private YmlManager ymlManager;

    private Flow flow;

    private Yml yml;

    @Before
    public void mockFlowAndYml() throws IOException {
        flow = flowService.create("hello");
        yml = flowService.saveYml(flow, StringHelper.toString(load("flow.yml")));
    }

    @Before
    public void userLogin() {
        mockLogin();
    }

    @Test
    public void should_start_new_job() throws Throwable {
        ObjectWrapper<Job> receivedJob = new ObjectWrapper<>();

        // init: register JobReceivedEvent
        CountDownLatch waitForJobFromQueue = new CountDownLatch(1);
        applicationEventMulticaster.addApplicationListener((ApplicationListener<JobReceivedEvent>) event -> {
            receivedJob.setValue(event.getJob());
            waitForJobFromQueue.countDown();
        });

        // when: create and start job
        Job job = jobService.create(flow, yml, Trigger.MANUAL, VariableMap.EMPTY);
        NodeTree tree = ymlManager.getTree(job);

        Assert.assertEquals(Status.PENDING, job.getStatus());
        Assert.assertEquals(tree.getRoot().getPath(), NodePath.create(job.getCurrentPath()));

        job = jobService.start(job);
        Assert.assertEquals(Status.ENQUEUE, job.getStatus());

        Assert.assertNotNull(job);
        Assert.assertNotNull(ymlManager.getTree(job));

        // then: confirm job is received from queue
        waitForJobFromQueue.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(0, waitForJobFromQueue.getCount());
        Assert.assertEquals(job, receivedJob.getValue());
    }

    @Test
    public void should_get_job_expire() {
        Job job = jobService.create(flow, yml, Trigger.MANUAL, VariableMap.EMPTY);
        Assert.assertFalse(jobService.isExpired(job));
    }

    @Test
    public void should_dispatch_job_to_agent() throws InterruptedException {
        // init:
        Job job = jobService.create(flow, yml, Trigger.MANUAL, VariableMap.EMPTY);
        Agent agent = agentService.create("hello.agent", null);
        mockAgentOnline(agentService.getPath(agent));

        // when:
        ObjectWrapper<Agent> targetAgent = new ObjectWrapper<>();
        ObjectWrapper<Cmd> targetCmd = new ObjectWrapper<>();
        CountDownLatch counter = new CountDownLatch(1);

        applicationEventMulticaster.addApplicationListener((ApplicationListener<CmdSentEvent>) event -> {
            targetAgent.setValue(event.getAgent());
            targetCmd.setValue(event.getCmd());
            counter.countDown();
        });

        jobService.processJob(job);

        // then: verify cmd been sent
        counter.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(agent, targetAgent.getValue());

        // then: verify cmd content
        Node root = YmlParser.load(flow.getName(), yml.getRaw());
        NodeTree tree = NodeTree.create(root);
        Node first = tree.next(tree.getRoot().getPath());

        Cmd cmd = targetCmd.getValue();
        Assert.assertEquals(cmdManager.createId(job, first).toString(), cmd.getId());
        Assert.assertEquals("echo step version", cmd.getInputs().getString("FLOW_VERSION"));
        Assert.assertEquals("echo step", cmd.getInputs().getString("FLOW_WORKSPACE"));
        Assert.assertEquals("echo hello\n", cmd.getScripts().get(0));
    }

    @Test
    public void should_handle_cmd_callback_for_success_status() {
        // init: agent and job
        Agent agent = agentService.create("hello.agent", null);
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.next(tree.getRoot().getPath());

        // when: cmd of first node been executed
        VariableMap output = new VariableMap();
        output.putString("HELLO_WORLD", "hello.world");

        ExecutedCmd executedCmd = new ExecutedCmd(cmdManager.createId(job, firstNode).toString(),
            firstNode.isAllowFailure());
        executedCmd.setStatus(ExecutedCmd.Status.SUCCESS);
        executedCmd.setOutput(output);

        jobService.processCallback(executedCmd);

        // then: executed cmd should be saved
        ExecutedCmd saved = executedCmdDao.findById(executedCmd.getId()).get();
        Assert.assertNotNull(saved);
        Assert.assertEquals(executedCmd, saved);

        // then: job context should be updated
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals("hello.world", job.getContext().getString("HELLO_WORLD"));

        // then: job current context should be updated
        Node secondNode = tree.next(firstNode.getPath());
        Assert.assertEquals(secondNode.getPath(), NodePath.create(job.getCurrentPath()));

        // when: cmd of second node been executed
        output = new VariableMap();
        output.putString("HELLO_JAVA", "hello.java");

        executedCmd = new ExecutedCmd(cmdManager.createId(job, secondNode).toString(), secondNode.isAllowFailure());
        executedCmd.setStatus(ExecutedCmd.Status.SUCCESS);
        executedCmd.setOutput(output);

        jobService.processCallback(executedCmd);

        // then: executed cmd of second node should be saved
        saved = executedCmdDao.findById(executedCmd.getId()).get();
        Assert.assertNotNull(saved);
        Assert.assertEquals(executedCmd, saved);

        // then: job context should be updated
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals("hello.java", job.getContext().getString("HELLO_JAVA"));
        Assert.assertEquals("hello.world", job.getContext().getString("HELLO_WORLD"));
        Assert.assertEquals(Status.SUCCESS, job.getStatus());
    }

    @Test
    public void should_handle_cmd_callback_for_failure_status_but_allow_failure() throws IOException {
        // init: agent and job
        yml = flowService.saveYml(flow, StringHelper.toString(load("flow-all-failure.yml")));
        Agent agent = agentService.create("hello.agent", null);
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.next(tree.getRoot().getPath());

        // when: cmd of first node with failure
        VariableMap output = new VariableMap();
        output.putString("HELLO_WORLD", "hello.world");

        ExecutedCmd executedCmd = new ExecutedCmd(cmdManager.createId(job, firstNode).toString(),
            firstNode.isAllowFailure());
        executedCmd.setStatus(ExecutedCmd.Status.EXCEPTION);
        executedCmd.setOutput(output);

        jobService.processCallback(executedCmd);

        // then: executed cmd should be recorded
        Assert.assertNotNull(executedCmdDao.findById(executedCmd.getId()).get());

        // then: job status should be running and current path should be change to second node
        job = jobDao.findById(job.getId()).get();
        Node secondNode = tree.next(firstNode.getPath());

        Assert.assertEquals(Status.RUNNING, job.getStatus());
        Assert.assertEquals(secondNode.getPathAsString(), job.getCurrentPath());
        Assert.assertEquals("hello.world", job.getContext().getString("HELLO_WORLD"));

        // when: second cmd of node been timeout
        output = new VariableMap();
        output.putString("HELLO_TIMEOUT", "hello.timeout");

        executedCmd = new ExecutedCmd(cmdManager.createId(job, secondNode).toString(), secondNode.isAllowFailure());
        executedCmd.setStatus(ExecutedCmd.Status.TIMEOUT);
        executedCmd.setOutput(output);

        jobService.processCallback(executedCmd);

        // then: executed cmd of second node should be recorded
        Assert.assertNotNull(executedCmdDao.findById(executedCmd.getId()).get());

        // then: job should be timeout with error message
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals(Status.SUCCESS, job.getStatus());
        Assert.assertEquals("hello.timeout", job.getContext().getString("HELLO_TIMEOUT"));
    }

    @Test
    public void should_job_failure_with_final_node() throws Exception {
        yml = flowService.saveYml(flow, StringHelper.toString(load("flow-failure-with-final.yml")));
        Agent agent = agentService.create("hello.agent", null);
        Job job = prepareJobForRunningStatus(agent);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.next(tree.getRoot().getPath());

        // when: set first step as failure status
        String cmdId = cmdManager.createId(job, firstNode).toString();
        ExecutedCmd executedCmd = new ExecutedCmd(cmdId, firstNode.isAllowFailure());
        executedCmd.setStatus(ExecutedCmd.Status.EXCEPTION);
        jobService.processCallback(executedCmd);

        // when: set final node as success status
        Node secondNode = tree.next(firstNode.getPath());
        cmdId = cmdManager.createId(job, secondNode).toString();
        executedCmd = new ExecutedCmd(cmdId, secondNode.isAllowFailure());
        executedCmd.setStatus(ExecutedCmd.Status.SUCCESS);
        jobService.processCallback(executedCmd);

        // then: job status should be failure since final node does not count to step
        job = jobDao.findById(job.getId()).get();
        Assert.assertEquals(Status.FAILURE, job.getStatus());
    }

    private Job prepareJobForRunningStatus(Agent agent) {
        // init: job to mock the first node been send to agent
        Job job = jobService.create(flow, yml, Trigger.MANUAL, VariableMap.EMPTY);

        NodeTree tree = ymlManager.getTree(job);
        Node firstNode = tree.next(tree.getRoot().getPath());

        job.setAgentId(agent.getId());
        job.setCurrentPath(firstNode.getPath().getPathInStr());
        job.setStatus(Status.RUNNING);

        return jobDao.save(job);
    }
}