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

package com.flowci.core.agent;

import com.flowci.core.config.ConfigProperties;
import com.flowci.domain.Agent;
import com.flowci.zookeeper.ZookeeperClient;
import com.flowci.zookeeper.ZookeeperException;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class AgentServiceImpl implements AgentService {

    @Autowired
    private ConfigProperties config;

    @Autowired
    private ZookeeperClient client;

    @Autowired
    private AgentDao agentDao;

    @PostConstruct
    public void initRootNode() {
        try {
            String root = config.getZookeeper().getRoot();
            client.create(CreateMode.PERSISTENT, root, null);
            log.info("The root node {} been initialized", root);
        } catch (ZookeeperException ignore) {

        }
    }

    @Override
    public Agent get(String id) {
        return agentDao.findById(id).get();
    }

    @Override
    public Agent create(String name, Set<String> tags) {
        Agent agent = new Agent(name, tags);
        agent.setToken(UUID.randomUUID().toString());
        return agentDao.save(agent);
    }
}
