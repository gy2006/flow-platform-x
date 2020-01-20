package com.flowci.core.agent.domain;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "agent_host")
public class LocalUnixAgentHost extends AgentHost {
    
}