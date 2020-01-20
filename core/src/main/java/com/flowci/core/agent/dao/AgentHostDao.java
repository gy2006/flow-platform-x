package com.flowci.core.agent.dao;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.flowci.core.agent.domain.AgentHost;

@Repository
public interface AgentHostDao extends MongoRepository<AgentHost, String> {

}