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

package com.flowci.agent.test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.flowci.agent.config.AgentProperties;
import com.flowci.agent.test.SpringScenario.Config;
import com.flowci.domain.Agent;
import com.flowci.domain.Jsonable;
import com.flowci.domain.Settings;
import com.flowci.domain.http.ResponseMessage;
import com.flowci.zookeeper.ZookeeperClient;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.CreateMode;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yang
 */
@Log4j2
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = Config.class)
public abstract class SpringScenario {

    static final String RootNode = "/flow-agent-test";

    static final String AgentID = "agent-id-123";

    static final String Token = "123-123-123";

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(8088);

    @ClassRule
    public static ZookeeperClientRule zkRule = new ZookeeperClientRule("127.0.0.1:2181");

    @BeforeClass
    public static void mockAgentSettings() throws Throwable {
        Settings.RabbitMQ mq = new Settings.RabbitMQ();
        mq.setHost("127.0.0.1");
        mq.setPort(5672);
        mq.setUsername("guest");
        mq.setPassword("guest");

        Settings.Zookeeper zk = new Settings.Zookeeper();
        zk.setHost("127.0.0.1:2181");
        zk.setRoot(RootNode);

        Agent local = new Agent("hello.agent");
        local.setId(AgentID);
        local.setToken(Token);
        local.setTags(Sets.newHashSet("local", "test"));

        Settings settings = new Settings(local, mq, zk, "queue.jobs.callback.test");
        ResponseMessage<Settings> responseBody = new ResponseMessage<>(200, settings);

        stubFor(get(urlPathEqualTo("/agents/connect"))
            .withQueryParam("token", equalTo(Token))
            .willReturn(aResponse()
                .withBody(Jsonable.getMapper().writeValueAsBytes(responseBody))
                .withHeader("Content-Type", "application/json")));
    }

    @BeforeClass
    public static void createRootNode() {
        try {
            zkRule.getClient().create(CreateMode.PERSISTENT, "/flow-agent-test", null);
        } catch (Throwable ignore) {

        }
    }

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        public AgentProperties mock() throws IOException {
            File mockWorkspace = folder.newFolder("workspace");
            File mockLogDir = folder.newFolder("logs");

            AgentProperties p = new AgentProperties();
            p.setWorkspace(mockWorkspace.toString());
            p.setLoggingDir(mockLogDir.toString());
            p.setServerUrl("http://localhost:8088");
            p.setToken(Token);
            return p;
        }
    }

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    protected ApplicationEventMulticaster applicationEventMulticaster;

    @Before
    public void reset() {
        WireMock.reset();
    }

    @Before
    public void resetEventListener() {
        applicationEventMulticaster.removeAllListeners();
    }

    @After
    public void cleanNodes() {
        for (String child : zk.children(RootNode)) {
            zk.delete(RootNode + "/" + child, true);
        }
    }

    protected InputStream load(String resource) {
        return SpringScenario.class.getClassLoader().getResourceAsStream(resource);
    }
}
