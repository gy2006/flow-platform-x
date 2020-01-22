package com.flowci.pool.domain;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor(staticName = "of")
public class AgentContainer {

    public static final String Image = "flowci/agent:latest";

    public static final String DefaultPrefix = "flowci";

    public static String buildName(String agentName, String flag) {
        return String.format("%s.%s", buildPrefix(flag), agentName);
    }

    public static String buildPrefix(String flag) {
        return String.format("%s-%s", DefaultPrefix, flag);
    }

    private final String id;

    private final String name;

    private final String state;

    public String getAgentName() {
        int index = name.lastIndexOf(".");
        if (index == -1) {
            throw new IllegalArgumentException("Cannot get agent name from container name");
        }
        return name.substring(index + 1);
    }
}