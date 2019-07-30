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

import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.common.helper.RabbitBuilder;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.test.SpringScenario.Config;
import com.flowci.core.test.flow.FlowMockHelper;
import com.flowci.core.user.CurrentUserHelper;
import com.flowci.core.user.User;
import com.flowci.core.user.UserService;
import com.flowci.domain.Agent;
import java.io.InputStream;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yang
 */
@Log4j2
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
@AutoConfigureMockMvc
public abstract class SpringScenario {

    @TestConfiguration
    public static class Config {

        @Bean("mvcMockHelper")
        public MvcMockHelper mvcMockHelper() {
            return new MvcMockHelper();
        }

        @Bean("flowMockHelper")
        public FlowMockHelper flowMockHelper() {
            return new FlowMockHelper();
        }
    }

    @Autowired
    protected CurrentUserHelper currentUserHelper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private String callbackQueue;

    @Autowired
    private String loggingQueue;

    @Autowired
    private RabbitBuilder jobQueueManager;

    @Autowired
    private RabbitBuilder callbackQueueManager;

    @Autowired
    private RabbitBuilder loggingQueueManager;

    @Autowired
    private RabbitBuilder agentQueueManager;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    protected ApplicationEventMulticaster applicationEventMulticaster;

    @After
    public void cleanListeners() {
        applicationEventMulticaster.removeAllListeners();
    }

    @After
    public void dbCleanUp() {
        mongoTemplate.getDb().drop();
    }

    @After
    public void queueCleanUp() {
        callbackQueueManager.purge(callbackQueue);
        loggingQueueManager.purge(loggingQueue);

        for (Agent agent : agentDao.findAll()) {
            agentQueueManager.purge(agent.getQueueName());
        }

        for (Flow flow : flowDao.findAll()) {
            jobQueueManager.purge(flow.getQueueName());
        }
    }

    protected InputStream load(String resource) {
        return SpringScenario.class.getClassLoader().getResourceAsStream(resource);
    }

    protected void mockLogin() {
        User user = userService.getByEmail("test@flow.ci");
        if (Objects.isNull(user)) {
            user = userService.create("test@flow.ci", "12345");
        }
        currentUserHelper.set(user);
    }
}
