/*
 * Copyright 2018 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.core.agent;

import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * @author yang
 */
public interface AgentDao extends MongoRepository<Agent, String> {

    List<Agent> findAllByStatusAndTagsIn(Status status, Set<String> tags);

    List<Agent> findAllByStatus(Status status);

}
