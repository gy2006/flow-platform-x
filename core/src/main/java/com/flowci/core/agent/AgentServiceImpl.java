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
import java.util.Optional;
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
    private ConfigProperties.Zookeeper zkConfig;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @PostConstruct
    public void initRootNode() {
        String root = zkConfig.getRoot();

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
        Optional<Agent> optional = agentDao.findById(id);
        if (!optional.isPresent()) {
            throw new NotFoundException("Agent {0} does not existed", id);
        }
        return optional.get();
    }

    @Override
    public String getPath(Agent agent) {
        String root = zkConfig.getRoot();
        return root + Agent.PATH_SLASH + agent.getId();
    }

    @Override
    public Agent find(Status status, Set<String> tags) {
        List<Agent> agents;

        if (Objects.isNull(tags) || tags.isEmpty()) {
            agents = agentDao.findAllByStatus(status);
        } else {
            agents = agentDao.findAllByStatusAndTagsIn(status, tags);
        }

        if (agents.isEmpty()) {
            String tagsInStr = StringUtils.collectionToCommaDelimitedString(tags);
            throw new NotFoundException("Agent not found by status : {0} and tags: {1}", status.name(), tagsInStr);
        }

        return agents.get(0);
    }

    @Override
    public Boolean tryLock(Agent agent) {
        // check agent is available form db
        Agent reload = get(agent.getId());
        if (reload.isBusy()) {
            return false;
        }

        try {
            // check agent status from zk
            String zkPath = getPath(reload);
            Status status = Status.fromBytes(zk.get(zkPath));
            if (status != Status.IDLE) {
                return false;
            }

            // lock and set status to busy
            zk.lock(zkPath, path -> updateAgentStatus(reload, Status.BUSY));
            return true;
        } catch (ZookeeperException e) {
            log.debug(e);
            return false;
        }
    }

    @Override
    public void tryRelease(Agent agent) {
        Agent reload = get(agent.getId());
        if (reload.isIdle()) {
            return;
        }

        // TODO: send STOP cmd to agent
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

    /**
     * Update agent status from ZK and DB
     * @param agent
     * @param status
     */
    private void updateAgentStatus(Agent agent, Status status) {
        agent.setStatus(status);

        // update zookeeper status
        String path = getPath(agent);
        Status current = Status.fromBytes(zk.get(path));
        if (current != status) {
            zk.set(path, status.getBytes());
        }

        // update database status
        agentDao.save(agent);
        applicationEventPublisher.publishEvent(new StatusChangeEvent(this, agent));
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

            if (event.getType() == Type.CHILD_ADDED) {
                updateAgentStatus(agent, Status.IDLE);
                log.debug("Event '{}' of agent '{}' with status '{}'", Type.CHILD_ADDED, agent.getName(), Status.IDLE);
                return;
            }

            if (event.getType() == Type.CHILD_REMOVED) {
                updateAgentStatus(agent, Status.OFFLINE);
                log.debug("Event '{}' of agent '{}' with status '{}'", Type.CHILD_REMOVED, agent.getName(), Status.OFFLINE);
                return;
            }

            if (event.getType() == Type.CHILD_UPDATED) {
                byte[] statusInBytes = zk.get(event.getData().getPath());
                Status status = Status.fromBytes(statusInBytes);
                updateAgentStatus(agent, status);
                log.debug("Event '{}' of agent '{}' with status '{}'", Type.CHILD_UPDATED, agent.getName(), status);
            }
        }
    }
}
