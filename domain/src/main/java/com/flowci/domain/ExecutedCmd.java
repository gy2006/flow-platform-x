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
import static com.flowci.domain.ExecutedCmd.Status.REJECTED;
import static com.flowci.domain.ExecutedCmd.Status.STOPPED;
import static com.flowci.domain.ExecutedCmd.Status.SUCCESS;
import static com.flowci.domain.ExecutedCmd.Status.TIMEOUT_KILL;

import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Data
@Document
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
public final class ExecutedCmd implements Serializable {

    public final static Integer CODE_TIMEOUT = -100;

    private final static Set<Status> FailureStatus = ImmutableSet.of(
        EXCEPTION,
        KILLED,
        REJECTED,
        TIMEOUT_KILL,
        STOPPED
    );

    public enum Status {

        PENDING(-1),

        SUCCESS(2), // current cmd

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

    private String id;

    private Integer processId;

    private Status status = Status.PENDING;

    /**
     * Linux shell exit code
     */
    private Integer code;

    private VariableMap output = new VariableMap();

    private Long startAt;

    private Long finishAt;

    public ExecutedCmd(String id) {
        this.id = id;
    }

    public boolean isSuccess() {
        return status == SUCCESS;
    }

    public boolean isFailure() {
        return FailureStatus.contains(status);
    }

    public Long getDuration() {
        if (Objects.isNull(startAt) || Objects.isNull(finishAt)) {
            return -1L;
        }

        return finishAt - startAt;
    }
}
