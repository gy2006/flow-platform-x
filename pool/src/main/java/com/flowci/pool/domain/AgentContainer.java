package com.flowci.pool.domain;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor(staticName = "of")
public class AgentContainer {

    public static final String Image = "flowci/agent:latest";

    public static final String DefaultPerfix = "ci-agent";

    public static String buildName(String agentName, String flag) {
        return String.format("%s-%s.%s", DefaultPerfix, flag, agentName);
    }

    public static String buildPerfix(String flag) {
        return String.format("%s-%s", DefaultPerfix, flag);
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