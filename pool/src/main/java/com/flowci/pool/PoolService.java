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

package com.flowci.pool;

import com.flowci.pool.exception.PoolException;

/**
 * Automatic manage agents
 * 
 * @author yang
 */
public interface PoolService<Context extends PoolContext> {

    void start(Context context) throws PoolException;

    void status(Context context) throws PoolException;

    void stop(Context context) throws PoolException;

    void remove(Context context) throws PoolException;
}
