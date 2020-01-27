/*
 * Copyright 2020 flow.ci
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

package com.flowci.core.agent.event;

import com.flowci.core.agent.domain.AgentHost;
import com.flowci.domain.Agent;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AgentCreatedEvent extends ApplicationEvent {

    private final Agent agent;

    private final AgentHost host;

    public AgentCreatedEvent(Object source, Agent agent, AgentHost host) {
        super(source);
        this.agent = agent;
        this.host = host;
    }
}
