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

package com.flowci.core.test.agent;

import com.flowci.core.agent.AgentService;
import com.flowci.core.agent.event.StatusChangeEvent;
import com.flowci.core.config.ConfigProperties;
import com.flowci.core.test.ZookeeperScenario;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.domain.ObjectWrapper;
import com.flowci.zookeeper.ZookeeperClient;
import com.google.common.collect.ImmutableSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.zookeeper.CreateMode;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

/**
 * @author yang
 */
public class AgentServiceTest extends ZookeeperScenario {

    @Autowired
    private ConfigProperties config;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AgentService agentService;

    @Test
    public void should_init_root_node() {
        Assert.assertTrue(zk.exist(config.getZookeeper().getRoot()));
    }

    @Test
    public void should_create_agent_in_db() {
        Agent agent = agentService.create("hello.test", ImmutableSet.of("local", "android"));
        Assert.assertNotNull(agent);
        Assert.assertEquals(agent, agentService.get(agent.getId()));
    }

    @Test
    public void should_make_agent_online() throws InterruptedException {
        // init:
        Agent agent = agentService.create("hello.test", ImmutableSet.of("local", "android"));
        String agentPath = config.getZookeeper().getRoot() + "/" + agent.getId();

        CountDownLatch counter = new CountDownLatch(1);
        ObjectWrapper<Agent> agentWrapper = new ObjectWrapper<>();
        applicationEventMulticaster.addApplicationListener((ApplicationListener<StatusChangeEvent>) event -> {
            counter.countDown();
            agentWrapper.setValue(event.getAgent());
        });

        // when: agent online in dummy
        zk.create(CreateMode.EPHEMERAL, agentPath, Status.IDLE.getBytes());

        // then:
        counter.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(agent, agentWrapper.getValue());
        Assert.assertEquals(Status.IDLE, agentWrapper.getValue().getStatus());
    }

    @Test
    public void should_find_available_agents() {
        // init:
        agentService.create("hello.test.1", ImmutableSet.of("local", "android"));
        agentService.create("hello.test.2", null);
        agentService.create("hello.test.3", ImmutableSet.of("alicloud", "android"));

        // when:
        Agent agent = agentService.find(Status.OFFLINE, ImmutableSet.of("android"));
        Assert.assertNotNull(agent);
    }
}
