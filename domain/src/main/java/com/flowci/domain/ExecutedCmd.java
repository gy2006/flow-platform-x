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
import static com.flowci.domain.ExecutedCmd.Status.PENDING;
import static com.flowci.domain.ExecutedCmd.Status.RUNNING;
import static com.flowci.domain.ExecutedCmd.Status.SKIPPED;
import static com.flowci.domain.ExecutedCmd.Status.SUCCESS;
import static com.flowci.domain.ExecutedCmd.Status.TIMEOUT;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ExecutedCmd == Step node with executed status and data
 *
 * @author yang
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExecutedCmd extends CmdBase {

    public final static Integer CODE_TIMEOUT = -100;

    public final static Integer CODE_SUCCESS = 0;

    private final static Set<Status> FailureStatus = ImmutableSet.of(
        EXCEPTION,
        KILLED,
        TIMEOUT
    );

    private final static Set<Status> SuccessStatus = ImmutableSet.of(
        SUCCESS,
        SKIPPED
    );

    public enum Status {

        PENDING(-1),

        RUNNING(1),

        SUCCESS(2),

        SKIPPED(2),

        EXCEPTION(3),

        KILLED(3),

        TIMEOUT(4);

        @Getter
        private Integer level;

        Status(Integer level) {
            this.level = level;
        }
    }

    /**
     * Flow id
     */
    private String flowId;

    /**
     * Job build number
     */
    private Long buildNumber;

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
    private Vars<String> output = new StringVars();

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

    /**
     * Num of line of the log
     */
    private Long logSize = -1L;

    private CmdId cmdId;

    public ExecutedCmd(CmdId cmdId, String flowId, boolean allowFailure) {
        checkNotNull(cmdId);

        setId(cmdId.toString());
        setFlowId(flowId);
        setAllowFailure(allowFailure);

        this.cmdId = cmdId;
    }

    @JsonIgnore
    public String getJobId() {
        return cmdId.getJobId();
    }

    @JsonIgnore
    public String getNodePath() {
        return cmdId.getNodePath();
    }

    @JsonIgnore
    public boolean isSuccess() {
        return SuccessStatus.contains(status) || isAllowFailure();
    }

    @JsonIgnore
    public boolean isRunning() {
        return status == RUNNING;
    }

    @JsonIgnore
    public boolean isPending() {
        return status == PENDING;
    }
}
