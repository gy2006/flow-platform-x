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

import com.flowci.domain.Agent;
import java.util.Set;

/**
 * @author yang
 */
public interface AgentService {

    /**
     * Get agent by id
     */
    Agent get(String id);

    /**
     * Find agent by status and tags from database
     *
     * @param status Status
     * @param tags Agent tags, optional
     */
    Agent find(Agent.Status status, Set<String> tags);

    /**
     * Try to occupy agent resource
     */
    Boolean occupy(Agent agent);

    /**
     * Release agent
     */
    void release(Agent agent);

    /**
     * Create agent by name and tags
     */
    Agent create(String name, Set<String> tags);

}
