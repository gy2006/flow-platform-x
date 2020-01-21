package com.flowci.pool.manager;

import static com.flowci.pool.domain.StartContext.AgentEnvs.AGENT_LOG_LEVEL;
import static com.flowci.pool.domain.StartContext.AgentEnvs.AGENT_TOKEN;
import static com.flowci.pool.domain.StartContext.AgentEnvs.SERVER_URL;

import java.util.ArrayList;
import java.util.List;

import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.DockerStatus;
import com.flowci.pool.domain.SocketInitContext;
import com.flowci.pool.domain.StartContext;
import com.flowci.pool.exception.PoolException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.google.common.collect.Lists;

public class SocketPoolManager implements PoolManager<SocketInitContext> {

    private DockerClient client;

    /**
     * Init local docker.sock api interface
     */
    @Override
    public void init(SocketInitContext context) throws Exception {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(context.getDockerHost()).build();

        client = DockerClientBuilder.getInstance(config).build();
    }

    @Override
    public List<AgentContainer> list() {
        final String nameFilter = AgentContainer.Perfix + "*";

        List<Container> list = client.listContainersCmd().withShowAll(true)
                .withNameFilter(Lists.newArrayList(nameFilter)).exec();

        List<AgentContainer> result = new ArrayList<>(list.size());
        for (Container item : list) {
            result.add(AgentContainer.of(item.getId(), item.getNames()[0], item.getState()));
        }
        return result;
    }

    @Override
    public int size() {
        final String nameFilter = AgentContainer.Perfix + "*";
        return client.listContainersCmd().withShowAll(true).withNameFilter(Lists.newArrayList(nameFilter)).exec()
                .size();
    }

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public void start(StartContext context) throws PoolException {
        final String name = buildName(context.getToken());

        CreateContainerResponse container = client.createContainerCmd(AgentContainer.Image).withName(name)
                .withEnv(String.format("%s=%s", SERVER_URL, context.getServerUrl()),
                        String.format("%s=%s", AGENT_TOKEN, context.getToken()),
                        String.format("%s=%s", AGENT_LOG_LEVEL, context.getLogLevel()))
                .withVolumes(new Volume(context.getDirOnHost()), new Volume("/root/.flow.ci.agent"))
                .withVolumes(new Volume("/var/run/docker.sock"), new Volume("/var/run/docker.sock")).exec();

        client.startContainerCmd(container.getId()).exec();
    }

    @Override
    public void stop(String token) throws PoolException {
        client.stopContainerCmd(findContainer(token).getId()).exec();
    }

    @Override
    public void resume(String token) throws PoolException {
        Container c = findContainer(token);
        client.startContainerCmd(c.getId()).exec();
    }

    @Override
    public void remove(String token) throws PoolException {
        client.removeContainerCmd(findContainer(token).getId()).exec();
    }

    @Override
    public String status(String token) throws PoolException {
        try {
            return findContainer(token).getState();
        } catch (PoolException e) {
            return DockerStatus.None;
        }
    }

    private Container findContainer(String token) throws PoolException {
        String name = buildName(token);
        List<Container> list = client.listContainersCmd().withShowAll(true).withNameFilter(Lists.newArrayList(name))
                .exec();

        if (list.size() != 1) {
            throw new PoolException("Unable to find container for agent {0}", name);
        }

        return list.get(0);
    }

    private static String buildName(String token) {
        return String.format("%s-local-%s", AgentContainer.Perfix, token);
    }
}