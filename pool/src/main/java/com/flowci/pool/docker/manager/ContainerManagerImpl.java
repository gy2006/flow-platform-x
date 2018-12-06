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

package com.flowci.pool.docker.manager;

import com.flowci.pool.docker.CreateContainer;
import com.flowci.pool.docker.DockerConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class ContainerManagerImpl implements ContainerManager {

    @Override
    public String create(DockerConfig config, CreateContainer data) {
        DockerClient client = config.getClient();

        CreateContainerCmd containerCmd = client.createContainerCmd(data.getImage());
        data.bind(containerCmd);

        CreateContainerResponse response = containerCmd.exec();
        return response.getId();
    }

    @Override
    public void start(DockerConfig config, String containerId) {
        config.getClient().startContainerCmd(containerId).exec();
    }

    @Override
    public void stop(DockerConfig config, String containerId) {
        config.getClient().stopContainerCmd(containerId).exec();
    }

    @Override
    public void remove(DockerConfig config, String containerId) {
        config.getClient().removeContainerCmd(containerId).exec();
    }
}
