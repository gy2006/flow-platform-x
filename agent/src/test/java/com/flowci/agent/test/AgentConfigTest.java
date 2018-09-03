/*
 * Copyright 2018 fir.im
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

package com.flowci.agent.test;

import com.flowci.domain.Agent;
import com.flowci.domain.Settings;
import com.flowci.domain.Settings.RabbitMQ;
import com.flowci.domain.Settings.Zookeeper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class AgentConfigTest extends SpringScenario {

    @Autowired
    private Settings agentSettings;

    @Test
    public void should_load_settings_from_server() {
        Assert.assertNotNull(agentSettings);
        Assert.assertNotNull(agentSettings.getQueue());
        Assert.assertNotNull(agentSettings.getZookeeper());

        Zookeeper zookeeper = agentSettings.getZookeeper();
        Assert.assertEquals("127.0.0.1:2181", zookeeper.getHost());
        Assert.assertEquals("/flow-agent-test", zookeeper.getRoot());

        RabbitMQ queue = agentSettings.getQueue();
        Assert.assertEquals("127.0.0.1", queue.getHost());
        Assert.assertEquals(5672, (int) queue.getPort());
        Assert.assertEquals("guest", queue.getUsername());
        Assert.assertEquals("guest", queue.getPassword());

        Agent local = agentSettings.getAgent();
        Assert.assertEquals("agent.id.123", local.getId());
        Assert.assertEquals("hello.agent", local.getName());
        Assert.assertEquals("123-123-123", local.getToken());
        Assert.assertTrue(local.getTags().contains("local"));
        Assert.assertTrue(local.getTags().contains("test"));

        Assert.assertEquals("queue.jobs.callback.test", agentSettings.getCallbackQueueName());
    }
}
