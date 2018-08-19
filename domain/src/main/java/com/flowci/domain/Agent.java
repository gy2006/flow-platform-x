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

package com.flowci.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author yang
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class Agent implements Serializable {

    public final static String PATH_SLASH = "/";

    public enum Status {

        OFFLINE,

        BUSY,

        IDLE;

        public byte[] getBytes() {
            return this.toString().getBytes();
        }

        public static Status fromBytes(byte[] bytes) {
            return Status.valueOf(new String(bytes));
        }
    }

    private String id;

    private String name;

    private String token;

    private Set<String> tags = Collections.emptySet();

    private Status status = Status.OFFLINE;

    public Agent(String name) {
        this.name = name;
    }

    public Agent(String name, Set<String> tags) {
        this.name = name;
        this.tags = tags;
    }

    public String getQueueName() {
        return "queue.agent." + id;
    }
}
