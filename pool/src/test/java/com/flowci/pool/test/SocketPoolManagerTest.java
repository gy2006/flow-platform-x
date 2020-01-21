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

import com.flowci.pool.domain.DockerStatus;
import com.flowci.pool.domain.SocketInitContext;
import com.flowci.pool.domain.StartContext;
import com.flowci.pool.manager.PoolManager;
import com.flowci.pool.manager.SocketPoolManager;

import org.junit.Assert;
import org.junit.Test;


public class SocketPoolManagerTest extends PoolScenario {

    private final PoolManager<SocketInitContext> service = new SocketPoolManager();

    @Test
    public void should_start_agent_and_stop() throws Exception {
        service.init(new SocketInitContext());

        StartContext context = new StartContext();
        context.setServerUrl("http://localhost:8080");
        context.setToken("helloworld");

        service.start(context);
        Assert.assertEquals(DockerStatus.Running, service.status(context.getToken()));
        Assert.assertEquals(1, service.list().size());
        Assert.assertEquals(1, service.size());

        service.stop(context.getToken());
        Assert.assertEquals(DockerStatus.Exited, service.status(context.getToken()));
        Assert.assertEquals(1, service.list().size());
        Assert.assertEquals(1, service.size());

        service.remove(context.getToken());
        Assert.assertEquals(DockerStatus.None, service.status(context.getToken()));
        Assert.assertEquals(0, service.list().size());
        Assert.assertEquals(0, service.size());
    }
}