/*
 * Copyright 2020 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.flowci.core.agent.domain;

import java.util.HashSet;
import java.util.Set;

import com.flowci.core.common.domain.Mongoable;

import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
public abstract class AgentHost extends Mongoable {

    public enum Status {

        Connected,

        Disconnected
    }

    public enum Type {
        SSH,

        LocalUnixSocket,

        K8s
    }

    /**
     * Unique host name
     */
    @NonNull
    @Indexed(name = "index_agent_host_name", unique = true)
    private String name;

    /**
     * Agent host type
     */
    private Type type;

    /**
     * Host status
     */
    private Status status = Status.Disconnected;

    /**
     * Max agent size on the host
     */
    private int maxSize = 10;

    /**
     * Tags for all agent holed by host
     */
    private Set<String> tags = new HashSet<>();
}