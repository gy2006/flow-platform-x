package com.flowci.core.agent.domain;

import java.util.HashSet;
import java.util.Set;

import com.flowci.core.common.domain.Mongoable;

import org.springframework.data.mongodb.core.index.Indexed;

import lombok.Getter;
import lombok.Setter;

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

 @Getter
 @Setter
 public abstract class AgentHost extends Mongoable {

    public enum Status {

        Connected,

        Disonnected
    }

    /**
     * Unique host name
     */
    @Indexed(name = "index_agent_host_name", unique = true)
    private String name;

    /**
     * Host status
     */
    private Status status = Status.Disonnected;

    /**
     * Tags for all agent holed by host
     */
    private Set<String> tags = new HashSet<>();
 }