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

import com.flowci.domain.Agent;
import com.flowci.domain.Jsonable;
import com.flowci.domain.Settings;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Sets;
import java.io.InputStream;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yang
 */
@Log4j2
@RunWith(SpringRunner.class)
@SpringBootTest
public abstract class SpringScenario {

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(8088);

    @BeforeClass
    public static void mockAgentSettings() throws Throwable {
        Settings.RabbitMQ mq = new Settings.RabbitMQ();
        mq.setHost("127.0.0.1");
        mq.setPort(5672);
        mq.setUsername("guest");
        mq.setPassword("guest");

        Settings.Zookeeper zk = new Settings.Zookeeper();
        zk.setHost("127.0.0.1:2181");
        zk.setRoot("/flow-agent-test");

        String token = "123-123-123";

        Agent local = new Agent("hello.agent");
        local.setId("agent.id.123");
        local.setToken(token);
        local.setTags(Sets.newHashSet("local", "test"));

        String callbackQueueName = "queue.jobs.callback.test";

        stubFor(get(urlPathEqualTo("/agents"))
            .withQueryParam("token", equalTo(token))
            .willReturn(aResponse()
                .withBody(Jsonable.getMapper().writeValueAsBytes(new Settings(local, mq, zk, callbackQueueName)))
                .withHeader("Content-Type", "application/json")));
    }

    @Before
    public void reset() {
        WireMock.reset();
    }

    protected InputStream load(String resource) {
        return SpringScenario.class.getClassLoader().getResourceAsStream(resource);
    }
}
