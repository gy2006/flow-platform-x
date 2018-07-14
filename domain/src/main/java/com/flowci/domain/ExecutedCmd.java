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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yang
 */
@EqualsAndHashCode(of = {"id"})
public final class ExecutedCmd implements Serializable {

    public enum Status {

        PENDING(-1),

        SENT(0),

        RUNNING(1), // current cmd

        EXECUTED(2), // current cmd

        EXCEPTION(3),

        KILLED(3),

        REJECTED(3),

        TIMEOUT_KILL(4),

        STOPPED(4);

        @Getter
        private Integer level;

        Status(Integer level) {
            this.level = level;
        }
    }

    @Getter
    @Setter
    private String id;

    @Getter
    @Setter
    private Status status = Status.PENDING;

    /**
     * Linux shell exit code
     */
    @Getter
    @Setter
    private Integer code;

    @Getter
    @Setter
    private VariableMap output;

    @Getter
    @Setter
    private Long startAt;

    @Getter
    @Setter
    private Long finishAt;
}
