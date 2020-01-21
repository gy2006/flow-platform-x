package com.flowci.pool.domain;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor(staticName = "of")
public class AgentContainer {

    public static final String Image = "flowci/agent:latest";

    public static final String Perfix = "ci-agent-";

    private final String id;

    private final String name;

    private final String state;

}