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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.domain.CreateAgent;
import com.flowci.core.domain.StatusCode;
import com.flowci.core.test.MvcMockHelper;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.Agent;
import com.flowci.domain.Settings;
import com.flowci.domain.http.ResponseMessage;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * @author yang
 */
public class AgentControllerTest extends SpringScenario {

    private static final TypeReference<ResponseMessage<Agent>> AgentResponseType =
        new TypeReference<ResponseMessage<Agent>>() {
        };

    private static final TypeReference<ResponseMessage<Settings>> SettingsResponseType =
        new TypeReference<ResponseMessage<Settings>>() {
        };

    @Autowired
    private MvcMockHelper mvcMockHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void should_create_agent_and_connect() throws Throwable {
        // init:
        CreateAgent create = new CreateAgent();
        create.setName("hello.agent");
        create.setTags(Sets.newHashSet("test"));

        // when: request to create agent
        ResponseMessage<Agent> agentR = mvcMockHelper.expectSuccessAndReturnClass(post("/agents")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(create)), AgentResponseType);
        Assert.assertEquals(StatusCode.OK, agentR.getCode());

        // then: verify agent
        Agent agent = agentR.getData();
        Assert.assertNotNull(agent);
        Assert.assertNotNull(agent.getId());
        Assert.assertNotNull(agent.getToken());

        Assert.assertEquals("hello.agent", agent.getName());
        Assert.assertTrue(agent.getTags().contains("test"));

        // when: request to connect agent
        ResponseMessage<Settings> settingsR = mvcMockHelper.expectSuccessAndReturnClass(
            get("/agents/connect").param("token", agent.getToken()), SettingsResponseType);
        Assert.assertEquals(StatusCode.OK, settingsR.getCode());

        // then:
        Settings settings = settingsR.getData();
        Assert.assertEquals("queue.jobs.callback-test", settings.getCallbackQueueName());

        Assert.assertEquals("/flow-agents-test", settings.getZookeeper().getRoot());
        Assert.assertEquals("127.0.0.1:2181", settings.getZookeeper().getHost());

        Assert.assertEquals("127.0.0.1", settings.getQueue().getHost());
        Assert.assertEquals(5672, settings.getQueue().getPort().intValue());
        Assert.assertEquals("guest", settings.getQueue().getUsername());
        Assert.assertEquals("guest", settings.getQueue().getPassword());
    }
}