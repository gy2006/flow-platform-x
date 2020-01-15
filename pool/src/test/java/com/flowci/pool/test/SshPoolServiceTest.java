package com.flowci.pool.test;

import java.io.InputStream;

import com.flowci.pool.PoolService; 
import com.flowci.pool.ssh.SshContext;
import com.flowci.pool.ssh.SshPoolServiceImpl;
import com.flowci.util.StringHelper;

import org.junit.Test;

public class SshPoolServiceTest extends PoolScenario {

    private final PoolService<SshContext> service = new SshPoolServiceImpl();

    @Test
    public void should_start_agent_via_ssh() throws Exception {
        // init: context
        InputStream pk = load("test.pk");
        SshContext context = SshContext.of(StringHelper.toString(pk), "10.0.2.4", "server");

        // when: start
        service.init(context);
        service.start(context);
    }
}