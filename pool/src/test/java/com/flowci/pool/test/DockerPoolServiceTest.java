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

package com.flowci.pool.test;

import com.flowci.pool.PoolService;
import com.flowci.pool.docker.DockerContext;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
@FixMethodOrder(MethodSorters.JVM)
public class DockerPoolServiceTest extends PoolScenario {

    private static final DockerContext Context = new DockerContext();

    static {
        Context.setHost("unix:///var/run/docker.sock");
        Context.setServerUri(URI.create("http://localhost:8080"));
        Context.setToken("asdfadfdsa");
    }

    @Autowired
    private PoolService<DockerContext> dockerPoolService;

    @Test
    public void should_start_docker_for_agent() {
        dockerPoolService.start(Context);
        Assert.assertNotNull(Context.getContainerId());

        dockerPoolService.status(Context);
        Assert.assertEquals(DockerContext.STATUS_RUNNING, Context.getStatus());
        Assert.assertNotNull(Context.getStartAt());
    }

    @Test
    public void should_stop_the_container() throws InterruptedException {
        dockerPoolService.stop(Context);

        TimeUnit.SECONDS.sleep(10);

        dockerPoolService.status(Context);
        Assert.assertEquals(DockerContext.STATUS_EXITED, Context.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void should_remove_the_container() {
        dockerPoolService.remove(Context);
        dockerPoolService.status(Context);
    }
}
