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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import com.flowci.core.agent.dao.AgentHostDao;
import com.flowci.core.agent.domain.AgentHost;
import com.flowci.core.agent.domain.LocalUnixAgentHost;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.domain.Agent;
import com.flowci.exception.NotAvailableException;
import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.DockerStatus;
import com.flowci.pool.domain.SocketInitContext;
import com.flowci.pool.domain.StartContext;
import com.flowci.pool.exception.PoolException;
import com.flowci.pool.manager.PoolManager;
import com.flowci.pool.manager.SocketPoolManager;
import com.flowci.util.StringHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class AgentHostServiceImpl implements AgentHostService {

    private final Map<Class<?>, AgentHostService> mapping = new HashMap<>(3);

    private final int MaxSize = 10;

    @Autowired
    private String serverUrl;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private AgentHostDao agentHostDao;

    @Autowired
    private AgentService agentService;

    @PostConstruct
    public void init() {
        mapping.put(LocalUnixAgentHost.class, new LocalUnixAgentHostService());
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
    public void start(AgentHost host) {
        mapping.get(host.getClass()).start(host);
    }

    private class LocalUnixAgentHostService implements AgentHostService {

        private PoolManager<SocketInitContext> manager = new SocketPoolManager();

        @Override
        public void create(AgentHost host) {
            if (!Files.exists(Paths.get("/var/run/docker.sock"))) {
                throw new NotAvailableException("No local docker socket");
            }

            if (agentHostDao.findAllByType(AgentHost.Type.LocalUnixSocket).size() > 0) {
                throw new NotAvailableException("Local unix socket agent host been created");
            }

            try {
                SocketInitContext context = new SocketInitContext();
                manager.init(context);

                host = (LocalUnixAgentHost) host;
                host.setCreatedAt(new Date());
                host.setCreatedBy(sessionManager.getUserId());
                agentHostDao.save(host);
            } catch (Exception e) {
                log.warn("Unable to create local unix socket agnet host: {}", e.getMessage());
                throw new NotAvailableException(e.getMessage());
            }
        }

        @Override
        public synchronized void start(AgentHost host) {
            // resume from stopped
            List<AgentContainer> list = manager.list(Optional.of(DockerStatus.Exited));
            if (list.size() > 0) {
                AgentContainer container = list.get(0);
                try {
                    manager.resume(container.getAgentName());
                } catch (PoolException e) {
                    log.warn("Unable to resume agent container {}", container.getName());
                }
                return;
            }

            if (manager.size() >= MaxSize) {
                log.warn("Unable to start agent since over the limit size {}", MaxSize);
                return;
            }

            // create new agent on agent host
            String name = String.format("%s-%s", host.getName(), StringHelper.randomString(5));
            Agent agent = agentService.create(name, host.getTags(), Optional.of(host.getId()));

            try {
                StartContext context = new StartContext();
                context.setServerUrl(serverUrl);
                context.setAgentName(agent.getName());
                context.setToken(agent.getToken());
                
                manager.start(context);
            } catch (PoolException e) {
                log.warn("Unable to start agent {}", agent.getName());
            }
        }

        public void stop(AgentHost host) {
            // stop idle agents
        }

        public void remove(AgentHost host) {
            // remove agent container from stopped
        }

        @Override
        public List<AgentHost> list() {
            throw new NoSuchMethodError("Not available here");
        }

    }
}