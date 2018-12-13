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

package com.flowci.pool.docker;

import com.flowci.pool.PoolContext.AgentEnvs;
import com.flowci.pool.PoolService;
import com.flowci.pool.docker.manager.ContainerManager;
import com.flowci.pool.docker.manager.ImageManager;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.google.common.base.Preconditions;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service("dockerPoolService")
public class DockerPoolService implements PoolService<DockerContext> {

    // 2018-12-12T10:22:24.806545284Z
    private static final SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private ImageManager imageManager;

    @Autowired
    private ContainerManager containerManager;

    @Override
    public void start(DockerContext context) {
        DockerConfig config = context.getConfig();

        if (context.hasContainer()) {
            config.getClient().startContainerCmd(context.getContainerId());
            return;
        }

        String image = context.getImage();
        boolean isPulled = imageManager.pull(config, image);
        if (!isPulled) {
            throw new IllegalStateException("Unable to pull docker image: " + image);
        }

        CreateContainer create = CreateContainer.of(image);
        create.addEnv(AgentEnvs.SERVER_URL, context.getServerUri().toString());
        create.addEnv(AgentEnvs.AGENT_PORT, context.getPort().toString());
        create.addEnv(AgentEnvs.AGENT_TOKEN, context.getToken());
        create.addEnv(AgentEnvs.AGENT_LOG_LEVEL, context.getLogLevel());

        create.addExposePort(context.getPort(), context.getPort());
        String containerId = containerManager.create(config, create);

        containerManager.start(config, containerId);

        context.setContainerId(containerId);
        context.setStartAt(new Date());
    }

    @Override
    public void status(DockerContext context) {
        InspectContainerResponse inspect = containerManager.inspect(context.getConfig(), context.getContainerId());

        try {
            context.setStatus(inspect.getState().getStatus());
            context.setStartAt(DateFormat.parse(inspect.getState().getStartedAt()));
        } catch (ParseException ignore) {
            // ignore if cannot get startAt from docker
        }
    }

    @Override
    public void stop(DockerContext context) {
        Preconditions.checkNotNull(context.getContainerId(), "Container id must be defined in context");
        containerManager.stop(context.getConfig(), context.getContainerId());
    }

    @Override
    public void remove(DockerContext context) {
        Preconditions.checkNotNull(context.getContainerId(), "Container id must be defined in context");
        containerManager.remove(context.getConfig(), context.getContainerId());
    }
}
