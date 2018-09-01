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

package com.flowci.core.test;

import com.flowci.core.agent.event.StatusChangeEvent;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.domain.ObjectWrapper;
import com.flowci.zookeeper.ZookeeperClient;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.CreateMode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

/**
 * @author yang
 */
public abstract class ZookeeperScenario extends SpringScenario {

    private static TestingServer server;

    @Autowired
    private ZookeeperClient zk;

    @ClassRule
    public static TemporaryFolder temp = new TemporaryFolder();

    @BeforeClass
    public static void start() throws Exception {
        server = new TestingServer(2181);
        server.start();
    }

    @AfterClass
    public static void close() throws IOException {
        server.close();
    }

    protected Agent mockAgentOnline(String agentPath) throws InterruptedException {
        CountDownLatch counter = new CountDownLatch(1);
        ObjectWrapper<Agent> wrapper = new ObjectWrapper<>();

        applicationEventMulticaster.addApplicationListener((ApplicationListener<StatusChangeEvent>) event -> {
            counter.countDown();
            wrapper.setValue(event.getAgent());
        });

        zk.create(CreateMode.PERSISTENT, agentPath, Status.IDLE.getBytes());
        counter.await(10, TimeUnit.SECONDS);

        Assert.assertNotNull(wrapper.getValue());
        Assert.assertEquals(Status.IDLE, wrapper.getValue().getStatus());
        return wrapper.getValue();
    }
}
