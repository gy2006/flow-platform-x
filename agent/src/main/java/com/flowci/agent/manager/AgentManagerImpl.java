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

package com.flowci.agent.manager;

import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.domain.Settings;
import com.flowci.exception.StatusException;
import com.flowci.zookeeper.ZookeeperClient;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class AgentManagerImpl implements AgentManager {

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private Settings agentSettings;

    @PostConstruct
    public void init() {
        register();
    }

    @Override
    public void register() {
        if (!hasRootNode()) {
            throw new StatusException("Please start server before start agent");
        }

        String path = getPath();
        zk.create(CreateMode.EPHEMERAL, path, Status.IDLE.getBytes());
        log.info("Agent {} been registered on zk", path);
    }

    @Override
    public boolean changeStatus(Status status) {
        String path = getPath();
        try {
            zk.set(path, status.getBytes());
            return true;
        } catch (Throwable e) {
            log.error("Cannot change agent status to {}", status);
            return false;
        }
    }

    private boolean hasRootNode() {
        String root = agentSettings.getZookeeper().getRoot();
        return zk.exist(root);
    }

    private String getPath() {
        Agent agent = agentSettings.getAgent();
        String root = agentSettings.getZookeeper().getRoot();
        return root + Agent.PATH_SLASH + agent.getId();
    }
}
