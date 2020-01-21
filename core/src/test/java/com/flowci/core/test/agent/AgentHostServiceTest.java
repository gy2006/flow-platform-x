package com.flowci.core.test.agent;

import com.flowci.core.agent.domain.AgentHost;
import com.flowci.core.agent.domain.LocalUnixAgentHost;
import com.flowci.core.agent.service.AgentHostService;
import com.flowci.core.test.SpringScenario;
import com.google.common.collect.Sets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AgentHostServiceTest extends SpringScenario {

    @Autowired
    private AgentHostService agentHostService;

    @Before
    public void login() {
        mockLogin();   
    }

    @Test
    public void should_enable_to_create_unix_local_host() {
        // when: create host
        AgentHost host = new LocalUnixAgentHost();
        host.setName("test-host");
        host.setTags(Sets.newHashSet("local", "test"));
        agentHostService.create(host);

        // then:
        Assert.assertNotNull(host.getId());
        Assert.assertEquals(1, agentHostService.list().size());
        Assert.assertEquals(host, agentHostService.list().get(0));
    }
}