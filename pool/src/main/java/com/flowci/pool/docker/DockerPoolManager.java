package com.flowci.pool.docker;

import static com.flowci.pool.PoolContext.AgentEnvs.SERVER_URL;

import java.util.List;

import static com.flowci.pool.PoolContext.AgentEnvs.AGENT_TOKEN;
import static com.flowci.pool.PoolContext.AgentEnvs.AGENT_LOG_LEVEL;

import com.flowci.pool.AbstractPoolManager;
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

public class DockerPoolManager extends AbstractPoolManager<DockerContext> {

    private DockerClient client;

    /**
     * Init local docker.sock api interface
     */
    @Override
    public void init(DockerContext context) throws Exception {
        // setup client instance
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(context.getDockerHost()).build();

        client = DockerClientBuilder.getInstance(config).build();

        // fetch existing containers
        final String nameFilter = PoolContext.ContainerNamePerfix + "*";
        List<Container> list = client.listContainersCmd().withShowAll(true)
                .withNameFilter(Lists.newArrayList(nameFilter)).exec();
        numOfAgent.set(list.size());
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public void start(DockerContext context) throws PoolException {
        if (numOfAgent.get() > max) {
            throw new PoolException("Num of agent over the limit {0}", Integer.toString(max));
        }

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
            numOfAgent.incrementAndGet();
            return;
        }

        Container exist = list.get(0);
        if (Objects.equal(PoolContext.DockerStatus.Running, exist.getState())) {
            return;
        }

        if (Objects.equal(PoolContext.DockerStatus.Exited, exist.getState())) {
            client.startContainerCmd(exist.getId());
            numOfAgent.incrementAndGet();
            return;
        }

        throw new PoolException("Unhandled docker status");
    }

    @Override
    public void stop(DockerContext context) throws PoolException {
        client.stopContainerCmd(findContainer(context.getContainerName()).getId()).exec();
        numOfAgent.decrementAndGet();
    }

    @Override
    public void remove(DockerContext context) throws PoolException {
        client.removeContainerCmd(findContainer(context.getContainerName()).getId()).exec();
        numOfAgent.decrementAndGet();
    }

    @Override
    public String status(DockerContext context) throws PoolException {
        try {
            return findContainer(context.getContainerName()).getState();
        } catch (PoolException e) {
            return PoolContext.DockerStatus.None;
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