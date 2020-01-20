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

package com.flowci.pool.test;

import com.flowci.pool.PoolContext;
import com.flowci.pool.PoolManager;
import com.flowci.pool.docker.DockerContext;
import com.flowci.pool.docker.DockerPoolManager;

import org.junit.Assert;
import org.junit.Test;


public class DockerPoolManagerTest extends PoolScenario {

    private final PoolManager<DockerContext> service = new DockerPoolManager();

    @Test
    public void should_start_agent_and_stop() throws Exception {
        DockerContext context = new DockerContext();
        context.setServerUrl("http://localhost:8080");
        context.setToken("helloworld");
        service.init(context);

        service.start(context);
        Assert.assertEquals(PoolContext.DockerStatus.Running, service.status(context));

        service.stop(context);
        Assert.assertEquals(PoolContext.DockerStatus.Exited, service.status(context));

        service.remove(context);
        Assert.assertEquals(PoolContext.DockerStatus.None, service.status(context));
    }

    @Test
    public void should_throw_exception_if_over_the_limit() {

    }
}