/*
 * Copyright 2020 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.flowci.core.agent.service;

import com.flowci.core.agent.dao.AgentDao;
import com.flowci.core.agent.dao.AgentHostDao;
import com.flowci.core.agent.domain.AgentHost;
import com.flowci.core.agent.domain.LocalUnixAgentHost;
import com.flowci.core.agent.event.CreateAgentEvent;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.NoIdleAgentEvent;
import com.flowci.core.user.domain.User;
import com.flowci.domain.Agent;
import com.flowci.exception.NotAvailableException;
import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.SocketInitContext;
import com.flowci.pool.domain.StartContext;
import com.flowci.pool.exception.PoolException;
import com.flowci.pool.manager.PoolManager;
import com.flowci.pool.manager.SocketPoolManager;
import com.flowci.util.StringHelper;
import com.flowci.zookeeper.ZookeeperClient;
import com.flowci.zookeeper.ZookeeperException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Log4j2
@Service
public class AgentHostServiceImpl implements AgentHostService {

    private final Map<Class<?>, OnCreate> mapping = new HashMap<>(3);

    private final Cache<AgentHost, PoolManager<?>> poolManagerCache =
            CacheHelper.createLocalCache(10, 600, new PoolManagerRemover());

    private String collectTaskZkPath;

    @Autowired
    private ConfigProperties appProperties;

    @Autowired
    private String serverUrl;

    @Autowired
    private AgentDao agentDao;

    @Autowired
    private AgentHostDao agentHostDao;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private ZookeeperClient zk;

    @Autowired
    private ConfigProperties.Zookeeper zkProperties;

    //====================================================================
    //        %% Public functions
    //====================================================================

    @PostConstruct
    public void init() {
        collectTaskZkPath = ZKPaths.makePath(zkProperties.getCronRoot(), "agent-host-collect");
        mapping.put(LocalUnixAgentHost.class, new OnLocalUnixAgentHostCreate());
    }

    @PostConstruct
    public void autoCreateLocalAgentHost() {
        if (!appProperties.isAutoLocalAgentHost()) {
            return;
        }

        LocalUnixAgentHost host = new LocalUnixAgentHost();

        try {
            create(host);
        } catch (NotAvailableException e) {
            log.warn("Fail to create default local agent host");
        }
    }

    @PostConstruct
    public void syncAgents() {
        for (AgentHost host : list()) {
            sync(host);
        }
    }

    @Override
    public void create(AgentHost host) {
        mapping.get(host.getClass()).create(host);
    }

    @Override
    public List<AgentHost> list() {
        return agentHostDao.findAll();
    }

    @Override
    public void sync(AgentHost host) {
        PoolManager<?> manager = getPoolManager(host);

        List<Agent> agentList = agentDao.findAllByHostId(host.getId());
        Set<AgentItemWrapper> agentSet = AgentItemWrapper.toSet(agentList);

        List<AgentContainer> containerList = manager.list(Optional.empty());
        Set<AgentItemWrapper> containerSet = AgentItemWrapper.toSet(containerList);

        containerSet.removeAll(agentSet);
        for (AgentItemWrapper item : containerSet) {
            try {
                manager.remove(item.getName());
                log.info("Agent {} has been cleaned up", item.getName());
            } catch (PoolException ignore) {

            }
        }
    }

    @Override
    public boolean start(AgentHost host) {
        PoolManager<?> manager = getPoolManager(host);

        List<Agent> agents = agentDao.findAllByHostId(host.getId());
        List<Agent> offlines = new LinkedList<>();

        // resume from stopped
        for (Agent agent : agents) {
            if (agent.getStatus() == Agent.Status.OFFLINE) {
                try {
                    manager.resume(agent.getName());
                    log.info("Agent {} been resumed", agent.getName());
                    return true;
                } catch (PoolException e) {
                    log.warn("Unable to resume agent {}", agent.getName());
                    offlines.add(agent);
                }
            }
        }

        // re-start from offlines
        for (Agent agent : offlines) {
            StartContext context = new StartContext();
            context.setServerUrl(serverUrl);
            context.setAgentName(agent.getName());
            context.setToken(agent.getToken());

            try {
                manager.start(context);
                log.info("Agent {} been started", agent.getName());
                return true;
            } catch (PoolException e) {
                log.warn("Unable to restart agent {}", agent.getName());
            }
        }

        // create new agent
        if (agents.size() < host.getMaxSize()) {
            String name = String.format("%s-%s", host.getName(), StringHelper.randomString(5));
            CreateAgentEvent syncEvent = new CreateAgentEvent(this, name, host.getTags(), host.getId());
            eventManager.publish(syncEvent);

            Agent agent = syncEvent.getCreated();

            StartContext context = new StartContext();
            context.setServerUrl(serverUrl);
            context.setAgentName(agent.getName());
            context.setToken(agent.getToken());

            try {
                manager.start(context);
                log.info("Agent {} been created and started", name);
                return true;
            } catch (PoolException e) {
                log.warn("Unable to start created agent {}", agent.getName());
                return false;
            }
        }

        log.warn("Unable to start agent since over the limit size {}", host.getMaxSize());
        return false;
    }

    @Override
    public int size(AgentHost host) {
        PoolManager<?> manager = getPoolManager(host);
        return manager.size();
    }

    @Override
    public void collect(AgentHost host) {
        List<Agent> list = agentDao.findAllByHostId(host.getId());

        for (Agent agent : list) {
            if (agent.getStatus() == Agent.Status.IDLE) {
                if (!removeIfTimeout(host, agent)) {
                    stopIfTimeout(host, agent);
                }
                continue;
            }

            if (agent.getStatus() == Agent.Status.OFFLINE) {
                removeIfTimeout(host, agent);
            }
        }
    }

    @Override
    public void removeAll(AgentHost host) {
        PoolManager<?> manager = getPoolManager(host);
        List<Agent> list = agentDao.findAllByHostId(host.getId());

        for (Agent agent : list) {
            try {
                manager.remove(agent.getName());
                agentDao.delete(agent);
                log.info("Agent {} been removed and deleted", agent.getName());
            } catch (PoolException e) {
                log.info("Unable to remove agent {}", agent.getName());
            }
        }
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void scheduleCollect() {
        try {
            if (!lock()) {
                return;
            }

            log.info("Start to collect agents from host");
            for (AgentHost host : list()) {
                collect(host);
            }
            log.info("Collection finished");
        } finally {
            clean();
        }
    }

    //====================================================================
    //        %% Internal events
    //====================================================================

    @EventListener
    public void onNoIdleAgent(NoIdleAgentEvent event) {
        Job job = event.getJob();
        Set<String> agentTags = job.getAgentSelector().getTags();

        List<AgentHost> hosts;
        if (agentTags.isEmpty()) {
            hosts = list();
        } else {
            hosts = agentHostDao.findAllByTagsIn(agentTags);
        }

        if (hosts.isEmpty()) {
            log.warn("Unable to find matched agent host for job {}", job.getId());
            return;
        }

        for (AgentHost host : hosts) {
            if (start(host)) {
                return;
            }
        }

        log.info("Unable to start agent from hosts");
    }

    //====================================================================
    //        %% Private functions
    //====================================================================

    private boolean stopIfTimeout(AgentHost host, Agent agent) {
        if (!host.isOverMaxIdleSeconds(agent.getStatusUpdatedAt())) {
            return false;
        }

        try {
            PoolManager<?> manager = getPoolManager(host);
            manager.stop(agent.getName());
            log.debug("Agent {} been stopped", agent.getName());
            return true;
        } catch (Exception e) {
            log.warn("Unable to stop idle agent {}", agent.getName());
            return false;
        }
    }

    private boolean removeIfTimeout(AgentHost host, Agent agent) {
        if (!host.isOverMaxOfflineSeconds(agent.getStatusUpdatedAt())) {
            return false;
        }

        try {
            PoolManager<?> manager = getPoolManager(host);
            manager.remove(agent.getName());
            log.debug("Agent {} been removed", agent.getName());
            return true;
        } catch (Exception e) {
            log.warn("Unable to remove offline agent {}", agent.getName());
            return false;
        }
    }

    /**
     * Load or init pool manager from local cache for each agent host
     */
    private PoolManager<?> getPoolManager(AgentHost host) {
        PoolManager<?> manager = poolManagerCache.get(host, (h) -> {
            try {
                if (h.getType() == AgentHost.Type.LocalUnixSocket) {
                    PoolManager<SocketInitContext> poolManager = new SocketPoolManager();
                    poolManager.init(new SocketInitContext());
                    return poolManager;
                }
            } catch (Exception e) {
                log.warn("Unable to init local unix agent host");
            }

            return null;
        });

        if (Objects.isNull(manager)) {
            throw new NotAvailableException("Cannot load pool manager for host {}", host.getName());
        }

        return manager;
    }

    /**
     * check zk and lock
     */
    private boolean lock() {
        try {
            zk.create(CreateMode.EPHEMERAL, collectTaskZkPath, null);
            return true;
        } catch (ZookeeperException e) {
            log.warn("Unable to init agent host collect task : {}", e.getMessage());
            return false;
        }
    }

    private void clean() {
        try {
            zk.delete(collectTaskZkPath, false);
        } catch (ZookeeperException ignore) {

        }
    }

    //====================================================================
    //        %% Private classes
    //====================================================================

    private interface OnCreate {

        void create(AgentHost host);
    }

    private class OnLocalUnixAgentHostCreate implements OnCreate {

        @Override
        public void create(AgentHost host) {
            if (hasCreated()) {
                throw new NotAvailableException("Local unix socket agent host been created");
            }

            if (!Files.exists(Paths.get("/var/run/docker.sock"))) {
                deleteIfExist();
                throw new NotAvailableException("Docker socket not available");
            }

            try {
                host.setName("localhost");
                host.setCreatedAt(new Date());
                host.setCreatedBy(User.DefaultSystemUser);
                agentHostDao.save(host);
            } catch (Exception e) {
                log.warn("Unable to create local unix socket agent host: {}", e.getMessage());
                throw new NotAvailableException(e.getMessage());
            }
        }

        private boolean hasCreated() {
            return agentHostDao.findAllByType(AgentHost.Type.LocalUnixSocket).size() > 0;
        }

        private void deleteIfExist() {
            List<AgentHost> hosts = agentHostDao.findAllByType(AgentHost.Type.LocalUnixSocket);
            if (hosts.isEmpty()) {
                return;
            }
            agentHostDao.deleteAll(hosts);
        }
    }

    @AllArgsConstructor(staticName = "of")
    private static class AgentItemWrapper {

        static <T> Set<AgentItemWrapper> toSet(List<T> list) {
            Set<AgentItemWrapper> set = new HashSet<>(list.size());
            Iterator<T> iterator = list.iterator();
            for (; iterator.hasNext(); ) {
                set.add(AgentItemWrapper.of(iterator.next()));
                iterator.remove();
            }
            return set;
        }

        private final Object object;

        public String getName() {
            if (object instanceof Agent) {
                return ((Agent) object).getName();
            }

            if (object instanceof AgentContainer) {
                return ((AgentContainer) object).getAgentName();
            }

            throw new IllegalArgumentException();
        }

        @Override
        public boolean equals(Object o) {
            return this.getName().equals(o);
        }

        @Override
        public int hashCode() {
            return this.getName().hashCode();
        }
    }

    @Log4j2
    private static class PoolManagerRemover implements RemovalListener<AgentHost, PoolManager<?>> {

        @Override
        public void onRemoval(@Nullable AgentHost agentHost,
                              @Nullable PoolManager<?> poolManager,
                              @Nonnull RemovalCause removalCause) {
            if (poolManager != null) {
                try {
                    poolManager.close();
                } catch (Exception e) {
                    log.warn("Unable to close agent host", e);
                }
            }

            if (agentHost != null) {
                log.info("Agent pool manager for host {} been closed", agentHost.getName());
            }
        }
    }
}