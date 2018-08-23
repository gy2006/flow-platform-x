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

import com.flowci.core.agent.event.StatusChangeEvent;
import com.flowci.core.config.ConfigProperties;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.exception.NotFoundException;
import com.flowci.zookeeper.ZookeeperClient;
import com.flowci.zookeeper.ZookeeperException;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author yang
 */
@Log4j2
@Service
public class AgentServiceImpl implements AgentService {

    @Autowired
    private ConfigProperties config;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @PostConstruct
    public void initRootNode() {
        String root = config.getZookeeper().getRoot();

        try {
            zk.create(CreateMode.PERSISTENT, root, null);
        } catch (ZookeeperException ignore) {

        }

        try {
            zk.watchChildren(root, new RootNodeListener());
        } catch (ZookeeperException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public Agent get(String id) {
        return agentDao.findById(id).get();
    }

    @Override
    public Agent find(Status status, Set<String> tags) {
        List<Agent> agents = agentDao.findAllByStatusAndTagsIn(status, tags);
        if (agents.isEmpty()) {
            String tagsInStr = StringUtils.collectionToCommaDelimitedString(tags);
            throw new NotFoundException("Agent not found by status : {0} and tags: {1}", status.name(), tagsInStr);
        }
        return agents.get(0);
    }

    @Override
    public Boolean occupy(Agent agent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void release(Agent agent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Agent create(String name, Set<String> tags) {
        Agent agent = new Agent(name, tags);
        agent.setToken(UUID.randomUUID().toString());
        return agentDao.save(agent);
    }

    /**
     * Get agent id from zookeeper path
     *
     * Ex: /agents/123123, should get 123123
     */
    private static String getAgentIdFromPath(String path) {
        int index = path.lastIndexOf(Agent.PATH_SLASH);
        return path.substring(index + 1);
    }

    private class RootNodeListener implements PathChildrenCacheListener {

        private final Set<Type> ChildOperations = ImmutableSet.of(
            Type.CHILD_ADDED,
            Type.CHILD_REMOVED,
            Type.CHILD_UPDATED
        );

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            ChildData data = event.getData();

            if (ChildOperations.contains(event.getType())) {
                handleAgentStatusChange(event);
            }
        }

        private void handleAgentStatusChange(PathChildrenCacheEvent event) {
            String agentId = getAgentIdFromPath(event.getData().getPath());

            Agent agent = get(agentId);
            if (Objects.isNull(agent)) {
                log.warn("Agent {} does not existed", agentId);
                return;
            }

            if (event.getType() == Type.CHILD_ADDED) {
                updateAgentStatus(agent, Status.IDLE);
                return;
            }

            if (event.getType() == Type.CHILD_REMOVED) {
                updateAgentStatus(agent, Status.OFFLINE);
                return;
            }

            if (event.getType() == Type.CHILD_UPDATED) {
                byte[] statusInBytes = zk.get(event.getData().getPath());
                Status status = Status.fromBytes(statusInBytes);
                updateAgentStatus(agent, status);
            }
        }

        private void updateAgentStatus(Agent agent, Status status) {
            agent.setStatus(status);
            agentDao.save(agent);
            applicationEventPublisher.publishEvent(new StatusChangeEvent(this, agent));
        }
    }
}
