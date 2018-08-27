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

package com.flowci.core.job.domain;

import com.flowci.core.domain.Mongoable;
import java.util.Date;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * @author yang
 */
@Data
@Document
public class Job extends Mongoable {

    public enum Trigger {

        /**
         * Scheduler trigger
         */
        SCHEDULER,

        /**
         * Api trigger
         */
        API,

        /**
         * Manual trigger
         */
        MANUAL,

        /**
         * Git push event
         */
        PUSH,

        /**
         * Git PR event
         */
        PR,

        /**
         * Git tag event
         */
        TAG
    }

    public enum Status {

        /**
         * Initial job state
         */
        PENDING,

        /**
         * Job been dispatched to agent
         */
        DISPATCHED,

        /**
         * Agent take over the job, and been start to execute
         */
        RUNNING,

        /**
         * Job been executed
         */
        SUCCESS,

        /**
         * Job been executed but failure
         */
        FAILURE,

        /**
         * Job been stopped by user
         */
        STOPPED,

        /**
         * Job execution time been over the expiredAt
         */
        TIMEOUT
    }

    /**
     * Job key is generated from {flow id}-{build number}
     */
    @Indexed(name = "index_job_key", unique = true)
    private String key;

    @Field("flow_id")
    private String flowId;

    @Field("build_number")
    private Long buildNumber;

    private Trigger trigger;

    private Status status = Status.PENDING;

    @Field("expire_at")
    private Date expireAt;

    @Field("agent_id")
    private String agentId;
}
