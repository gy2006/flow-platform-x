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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import com.flowci.core.agent.dao.AgentHostDao;
import com.flowci.core.agent.domain.AgentHost;
import com.flowci.core.agent.domain.LocalUnixAgentHost;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.domain.Agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AgentHostServiceImpl implements AgentHostService {

    private final Map<Class<?>, AgentHostService> mapping = new HashMap<>(3);

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private AgentHostDao agentHostDao;

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
    public Agent start(AgentHost host) {
        return null;
    }

    private class LocalUnixAgentHostService implements AgentHostService {

        @Override
        public void create(AgentHost host) {
            host = (LocalUnixAgentHost) host;
            host.setCreatedAt(new Date());
            host.setCreatedBy(sessionManager.getUserId());
            agentHostDao.save(host);
        }

        @Override
        public Agent start(AgentHost host) {
            return null;
        }

        @Override
        public List<AgentHost> list() {
            return null;
        }

    }
}