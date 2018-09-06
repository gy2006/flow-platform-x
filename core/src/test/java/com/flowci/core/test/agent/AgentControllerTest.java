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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.flowci.exception.ErrorCode;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
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

    private static final TypeReference<ResponseMessage<List<Agent>>> AgentListResponseType =
        new TypeReference<ResponseMessage<List<Agent>>>() {
        };

    private static final TypeReference<ResponseMessage<Settings>> SettingsResponseType =
        new TypeReference<ResponseMessage<Settings>>() {
        };

    @Autowired
    private MvcMockHelper mvcMockHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_list_agent() throws Throwable {
        Agent first = createAgent("first.agent", null);
        Agent second = createAgent("second.agent", null);

        ResponseMessage<List<Agent>> response =
            mvcMockHelper.expectSuccessAndReturnClass(get("/agents"), AgentListResponseType);
        Assert.assertEquals(StatusCode.OK, response.getCode());

        List<Agent> list = response.getData();
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.contains(first));
        Assert.assertTrue(list.contains(second));
    }

    @Test
    public void should_delete_agent() throws Throwable {
        Agent created = createAgent("should.delete", null);

        ResponseMessage<Agent> responseOfDeleteAgent =
            mvcMockHelper.expectSuccessAndReturnClass(delete("/agents/" + created.getToken()), AgentResponseType);
        Assert.assertEquals(StatusCode.OK, responseOfDeleteAgent.getCode());
        Assert.assertEquals(created, responseOfDeleteAgent.getData());

        ResponseMessage<Agent> responseOfGetAgent =
            mvcMockHelper.expectSuccessAndReturnClass(get("/agents/" + created.getToken()), AgentResponseType);
        Assert.assertEquals(ErrorCode.NOT_FOUND, responseOfGetAgent.getCode());
    }

    @Test
    public void should_create_agent_and_connect() throws Throwable {
        // init:
        Agent agent = createAgent("hello.agent", Sets.newHashSet("test"));

        // then: verify agent
        Assert.assertNotNull(agent);
        Assert.assertNotNull(agent.getId());
        Assert.assertNotNull(agent.getToken());

        Assert.assertEquals("hello.agent", agent.getName());
        Assert.assertTrue(agent.getTags().contains("test"));

        // when: request to connect agent
        currentUserHelper.reset();
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

    private Agent createAgent(String name, Set<String> tags) throws Exception {
        CreateAgent create = new CreateAgent();
        create.setName(name);
        create.setTags(tags);

        ResponseMessage<Agent> agentR = mvcMockHelper.expectSuccessAndReturnClass(post("/agents")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(create)), AgentResponseType);

        Assert.assertEquals(StatusCode.OK, agentR.getCode());

        return agentR.getData();
    }
}