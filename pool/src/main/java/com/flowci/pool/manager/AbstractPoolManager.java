/*
 * Copyright 2020 flow.ci
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

import java.util.concurrent.atomic.AtomicInteger;

import com.flowci.pool.domain.PoolContext;

public abstract class AbstractPoolManager<T extends PoolContext> implements PoolManager<T>{

    protected static final String Image = "flowci/agent:latest";

    protected int max = 10;

    protected AtomicInteger numOfAgent = new AtomicInteger(0);

    @Override
    public void setLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Max agent size must be positive integer");
        }
        max = limit;
    }

    @Override
    public int size() {
        return numOfAgent.get();
    }
}