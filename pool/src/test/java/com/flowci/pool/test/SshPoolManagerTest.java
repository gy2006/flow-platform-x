package com.flowci.pool.test;

import com.flowci.pool.domain.AgentContainer;
import com.flowci.pool.domain.DockerStatus;
import com.flowci.pool.domain.SshInitContext;
import com.flowci.pool.domain.StartContext;
import com.flowci.pool.exception.DockerPoolException;
import com.flowci.pool.manager.PoolManager;
import com.flowci.pool.manager.SshPoolManager;
import com.flowci.util.StringHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Optional;

public class SshPoolManagerTest extends PoolScenario {

    private final PoolManager<SshInitContext> manager = new SshPoolManager();

    @Before
    public void init() throws Exception {
        InputStream pk = load("test.pk");
        manager.init(SshInitContext.of(StringHelper.toString(pk), "10.0.2.4", "server", 10));
    }

    @After
    public void cleanup() throws DockerPoolException {
        for (AgentContainer c : manager.list(Optional.empty())) {
            manager.remove(c.getAgentName());
        }
    }

    @Test(expected = DockerPoolException.class)
    public void should_throw_exception_while_duplicate_container() throws DockerPoolException {
        String name = StringHelper.randomString(5);

        try {
            startAgent(name);
        } catch (DockerPoolException e) {
            Assert.fail();
        }

        startAgent(name);
    }

    @Test
    public void should_start_agent_and_stop() throws Exception {
        // when: start two agent
        String name1 = StringHelper.randomString(5);
        startAgent(name1);
        Assert.assertEquals(DockerStatus.Running, manager.status(name1));

        String name2 = StringHelper.randomString(5);
        startAgent(name2);
        Assert.assertEquals(DockerStatus.Running, manager.status(name2));

        // then: agent container should be running
        Assert.assertEquals(2, manager.list(Optional.empty()).size());
        Assert.assertEquals(2, manager.list(Optional.of(DockerStatus.Running)).size());
        Assert.assertEquals(2, manager.size());
    }

    private void startAgent(String name) throws DockerPoolException {
        StartContext context = new StartContext();
        context.setAgentName(name);
        context.setServerUrl("http://localhost:8080");
        context.setToken("helloworld");
        manager.start(context);
    }
}