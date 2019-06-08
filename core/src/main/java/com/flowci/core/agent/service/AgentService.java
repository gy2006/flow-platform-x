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

package com.flowci.core.agent.service;

import com.flowci.core.agent.domain.AgentInit;
import com.flowci.domain.Agent;
import com.flowci.domain.Cmd;
import com.flowci.domain.Settings;
import java.util.List;
import java.util.Set;

/**
 * @author yang
 */
public interface AgentService {

    Settings connect(AgentInit initData);

    /**
     * Get agent by id
     */
    Agent get(String id);

    /**
     * Get agent by token
     */
    Agent getByToken(String token);

    /**
     * Get zookeeper path
     */
    String getPath(Agent agent);

    /**
     * List agents
     */
    List<Agent> list();

    /**
     * Find agent by status and tags from database
     *
     * @param status Status
     * @param tags Agent tags, optional
     * @throws com.flowci.exception.NotFoundException
     */
    List<Agent> find(Agent.Status status, Set<String> tags);

    /**
     * Delete agent by token
     */
    Agent delete(String token);

    /**
     * Set agent tags by token
     */
    Agent setTags(String token, Set<String> tags);

    /**
     * Try to lock agent resource, and set agent status to BUSY
     */
    Boolean tryLock(Agent agent);

    /**
     * Release agent, send 'stop' cmd to agent
     */
    void tryRelease(Agent agent);

    /**
     * Create agent by name and tags
     */
    Agent create(String name, Set<String> tags);

    /**
     * Dispatch cmd to agent
     */
    void dispatch(Cmd cmd, Agent agent);

}
