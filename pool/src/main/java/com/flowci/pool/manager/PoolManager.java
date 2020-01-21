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

package com.flowci.pool.manager;

import java.util.List;

import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.InitContext;
import com.flowci.pool.domain.StartContext;
import com.flowci.pool.exception.PoolException;

/**
 * Handle agent in docker on different type of host
 * 
 * @author yang
 */
public interface PoolManager<T extends InitContext> extends AutoCloseable {

    /**
     * List all containers for agent
     */
    List<AgentContainer> list();

    /**
     * How many agent containers in the pool host
     */
    int size();

    /**
     * Init pool service setting
     */
    void init(T context) throws Exception;

    /**
     * Start an agent
     */
    void start(StartContext context) throws PoolException;

    /**
     * Stop an agent
     */
    void stop(String token) throws PoolException;

    /**
     * Resume agent container
     */
    void resume(String token) throws PoolException;

    /**
     * Remove an agent
     */
    void remove(String token) throws PoolException;

    /**
     * Get docker status
     */
    String status(String token) throws PoolException;
}
