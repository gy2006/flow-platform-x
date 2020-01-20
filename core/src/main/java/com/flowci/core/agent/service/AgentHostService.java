package com.flowci.core.agent.service;

import java.util.Set;

import com.flowci.core.agent.domain.AgentHost;
import com.flowci.domain.Agent;

public interface AgentHostService {

    /**
     * Create an agent host
     */
    AgentHost create(String name, Set<String> tags);

    /**
     * Start agent on the host
     */
    Agent start(AgentHost host);
}