package com.flowci.pool.test;

import com.flowci.pool.PoolContext;
import com.flowci.pool.PoolService;
import com.flowci.pool.docker.DockerContext;
import com.flowci.pool.docker.DockerPoolServiceImpl;

import org.junit.Assert;
import org.junit.Test;


public class DockerPoolServiceTest extends PoolScenario {

    private final PoolService<DockerContext> service = new DockerPoolServiceImpl();

    @Test
    public void should_start_agent_and_stop() throws Exception {
        DockerContext context = new DockerContext();
        context.setServerUrl("http://localhost:8080");
        context.setToken("helloworld");
        service.init(context);

        service.start(context);
        Assert.assertEquals(PoolContext.DockerStatus.Running, service.status(context));

        service.stop(context);
        Assert.assertEquals(PoolContext.DockerStatus.Exited, service.status(context));

        service.remove(context);
        Assert.assertEquals(PoolContext.DockerStatus.None, service.status(context));
    }
}