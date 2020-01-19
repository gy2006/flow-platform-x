package com.flowci.pool.docker;

import static com.flowci.pool.PoolContext.AgentEnvs.SERVER_URL;

import java.util.List;

import static com.flowci.pool.PoolContext.AgentEnvs.AGENT_TOKEN;
import static com.flowci.pool.PoolContext.AgentEnvs.AGENT_LOG_LEVEL;

import com.flowci.pool.AbstractPoolService;
import com.flowci.pool.PoolContext;
import com.flowci.pool.exception.PoolException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

public class DockerPoolServiceImpl extends AbstractPoolService<DockerContext> {

    private DockerClient client;

    /**
     * Init local docker.sock api interface
     */
    @Override
    public void init(DockerContext context) throws Exception {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock").build();

        client = DockerClientBuilder.getInstance(config).build();
    }

    @Override
    public void release(DockerContext context) throws Exception {
        // Do nothing, sicne only init local docker.sock api currently
    }

    @Override
    public void start(DockerContext context) throws PoolException {
        List<Container> list = client.listContainersCmd().withShowAll(true)
                .withNameFilter(Lists.newArrayList(context.getContainerName())).exec();

        if (list.isEmpty()) {
            CreateContainerResponse container = client.createContainerCmd(Image).withName(context.getContainerName())
                    .withEnv(String.format("%s=%s", SERVER_URL, context.getServerUrl()),
                            String.format("%s=%s", AGENT_TOKEN, context.getToken()),
                            String.format("%s=%s", AGENT_LOG_LEVEL, context.getLogLevel()))
                    .withVolumes(new Volume(context.getDirOnHost()), new Volume("/root/.flow.ci.agent"))
                    .withVolumes(new Volume("/var/run/docker.sock"), new Volume("/var/run/docker.sock")).exec();

            client.startContainerCmd(container.getId()).exec();
            return;
        }

        Container exist = list.get(0);
        if (Objects.equal(PoolContext.DockerStatus.Running, exist.getState())) {
            return;
        }

        if (Objects.equal(PoolContext.DockerStatus.Exited, exist.getState())) {
            client.startContainerCmd(exist.getId());
            return;
        }

        throw new PoolException("Unhandled docker status");
    }

    @Override
    public void stop(DockerContext context) throws PoolException {
        client.stopContainerCmd(findContainer(context.getContainerName()).getId()).exec();
    }

    @Override
    public void remove(DockerContext context) throws PoolException {
        client.removeContainerCmd(findContainer(context.getContainerName()).getId()).exec();
    }

    @Override
    public String status(DockerContext context) throws PoolException {
        try {
            return findContainer(context.getContainerName()).getState();
        } catch(PoolException e) {
            return PoolContext.DockerStatus.None;
        }
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    private Container findContainer(String name) throws PoolException {
        List<Container> list = client.listContainersCmd().withShowAll(true).withNameFilter(Lists.newArrayList(name))
                .exec();

        if (list.size() != 1) {
            throw new PoolException("Unable to find container for agent {0}", name);
        }

        return list.get(0);
    }
}