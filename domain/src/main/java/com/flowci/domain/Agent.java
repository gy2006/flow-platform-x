/*
 * Copyright 2018 fir.im
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

package com.flowci.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author yang
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Agent implements Serializable {

    private final static int DefaultTagSize = 10;

    public enum Status {

        OFFLINE,

        BUSY,

        IDLE
    }

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private String token;

    @Getter
    private Set<String> tags = new HashSet<String>(DefaultTagSize);

    @Getter
    @Setter
    private Status status = Status.OFFLINE;

    public Agent(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getQueueName() {
        return "queue.agent." + id;
    }

}
