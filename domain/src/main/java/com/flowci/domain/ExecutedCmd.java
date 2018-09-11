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

import static com.flowci.domain.ExecutedCmd.Status.EXCEPTION;
import static com.flowci.domain.ExecutedCmd.Status.KILLED;
import static com.flowci.domain.ExecutedCmd.Status.SUCCESS;
import static com.flowci.domain.ExecutedCmd.Status.TIMEOUT;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author yang
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public class ExecutedCmd implements Serializable {

    public final static Integer CODE_TIMEOUT = -100;

    private final static Set<Status> FailureStatus = ImmutableSet.of(
        EXCEPTION,
        KILLED,
        TIMEOUT
    );

    public enum Status {

        PENDING(-1),

        RUNNING(1),

        SUCCESS(2), // current cmd

        EXCEPTION(3),

        KILLED(3),

        TIMEOUT(4);

        @Getter
        private Integer level;

        Status(Integer level) {
            this.level = level;
        }
    }

    private String id;

    /**
     * Process id
     */
    private Integer processId;

    /**
     * Cmd execution status
     */
    private Status status = Status.PENDING;

    /**
     * Linux shell exit code
     */
    private Integer code;

    /**
     * Cmd output
     */
    private VariableMap output = new VariableMap();

    /**
     * Cmd start at timestamp
     */
    private Date startAt;

    /**
     * Cmd finish at timestamp
     */
    private Date finishAt;

    /**
     * Error message
     */
    private String error;

    public ExecutedCmd(String id) {
        this.id = id;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return status == SUCCESS;
    }

    @JsonIgnore
    public boolean isFailure() {
        return FailureStatus.contains(status);
    }

    @JsonIgnore
    public Long getDuration() {
        if (Objects.isNull(startAt) || Objects.isNull(finishAt)) {
            return -1L;
        }

        return finishAt.getTime() - startAt.getTime();
    }
}
