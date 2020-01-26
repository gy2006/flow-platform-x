package com.flowci.pool.manager;

import static com.flowci.pool.domain.AgentContainer.buildName;
import static com.flowci.pool.domain.AgentContainer.buildPrefix;
import static com.flowci.pool.domain.StartContext.AgentEnvs.AGENT_LOG_LEVEL;
import static com.flowci.pool.domain.StartContext.AgentEnvs.AGENT_TOKEN;
import static com.flowci.pool.domain.StartContext.AgentEnvs.SERVER_URL;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.DockerStatus;
import com.flowci.pool.domain.SocketInitContext;
import com.flowci.pool.domain.StartContext;
import com.flowci.pool.exception.PoolException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.google.common.collect.Lists;

public class SocketPoolManager implements PoolManager<SocketInitContext> {

    private static final String Flag = "socket";

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
    public List<AgentContainer> list(Optional<String> state) {
        final String nameFilter = buildPrefix(Flag) + "*";

        ListContainersCmd cmd = client.listContainersCmd().withShowAll(true)
                .withNameFilter(Lists.newArrayList(nameFilter));

        state.ifPresent((s) -> {
            cmd.withStatusFilter(Lists.newArrayList(state.get()));
        });

        List<Container> list = cmd.exec();
        List<AgentContainer> result = new ArrayList<>(list.size());
        for (Container item : list) {
            String name = item.getNames()[0];
            if (name.startsWith("/")) {
                name = name.substring(1);
            }
            result.add(AgentContainer.of(item.getId(), name, item.getState()));
        }
        return result;
    }

    @Override
    public int size() {
        final String nameFilter = buildPrefix(Flag) + "*";
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
        final String name = buildName(context.getAgentName(), Flag);

        CreateContainerResponse container = client.createContainerCmd(AgentContainer.Image).withName(name)
                .withEnv(String.format("%s=%s", SERVER_URL, context.getServerUrl()),
                        String.format("%s=%s", AGENT_TOKEN, context.getToken()),
                        String.format("%s=%s", AGENT_LOG_LEVEL, context.getLogLevel()))
                .withVolumes(new Volume(context.getDirOnHost()), new Volume("/root/.flow.ci.agent"))
                .withVolumes(new Volume("/var/run/docker.sock"), new Volume("/var/run/docker.sock")).exec();

        client.startContainerCmd(container.getId()).exec();
    }

    @Override
    public void stop(String name) throws PoolException {
        client.stopContainerCmd(findContainer(name).getId()).exec();
    }

    @Override
    public void resume(String name) throws PoolException {
        Container c = findContainer(name);
        client.startContainerCmd(c.getId()).exec();
    }

    @Override
    public void remove(String name) throws PoolException {
        client.removeContainerCmd(findContainer(name).getId()).withForce(true).exec();
    }

    @Override
    public String status(String name) throws PoolException {
        try {
            return findContainer(name).getState();
        } catch (PoolException e) {
            return DockerStatus.None;
        }
    }

    private Container findContainer(String name) throws PoolException {
        String containerName = buildName(name, Flag);
        List<Container> list = client.listContainersCmd().withShowAll(true).withNameFilter(Lists.newArrayList(containerName))
                .exec();

        if (list.size() != 1) {
            throw new PoolException("Unable to find container for agent {0}", containerName);
        }

        return list.get(0);
    }
}