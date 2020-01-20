package com.flowci.pool.domain;

import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@AllArgsConstructor(staticName = "of")
public class AgentContainer {

    private final String id;

    private final String name;

    private final String state;

}