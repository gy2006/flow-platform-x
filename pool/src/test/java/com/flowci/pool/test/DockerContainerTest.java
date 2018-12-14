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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.pool.docker.CreateContainer;
import com.flowci.pool.docker.DockerConfig;
import com.flowci.pool.docker.Network;
import com.flowci.pool.docker.manager.ContainerManager;
import com.flowci.pool.docker.manager.ImageManager;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import lombok.Getter;
import lombok.Setter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
@FixMethodOrder(MethodSorters.JVM)
public class DockerContainerTest extends PoolScenario {

    private static final String Image = "nginx:1.14";

    @Autowired
    private ImageManager imageManager;

    @Autowired
    private ContainerManager containerManager;

    @Before
    public void servicesCheck() {
        Assert.assertNotNull(containerManager);
    }

    @Test
    public void should_pull_image() {
        DockerConfig config = DockerConfig.of("unix:///var/run/docker.sock");
        Assert.assertTrue(imageManager.pull(config, Image));
    }

    @Test
    public void should_create_and_remove_container() throws IOException {
        URL html = getClass().getClassLoader().getResource("html");

        // prepare container params
        CreateContainer data = CreateContainer.of(Image);
        data.setNetwork(Network.Bridge);
        data.addExposePort(8000, 80);
        data.addVolume(html.getFile(), "/usr/share/nginx/html");
        data.addEnv("CREATE_FROM_API", "true");

        // create container
        DockerConfig config = DockerConfig.of("unix:///var/run/docker.sock");
        String containerId = containerManager.create(config, data);
        Assert.assertNotNull(containerId);

        // when: start container
        containerManager.start(config, containerId);

        // then:
        ObjectMapper mapper = new ObjectMapper();
        TestObj obj = mapper.readValue(URI.create("http://localhost:8000/hello.json").toURL(), TestObj.class);
        Assert.assertNotNull(obj);
        Assert.assertEquals("hello", obj.name);

        // when: stop container
        containerManager.stop(config, containerId);
        containerManager.remove(config, containerId);
    }

    @Getter
    @Setter
    public static class TestObj {

        private String name;
    }
}
