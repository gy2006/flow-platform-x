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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.core.common.domain.Variables;
import com.flowci.domain.Agent;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.store.Pathable;
import com.flowci.tree.Selector;
import com.flowci.util.StringHelper;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 * @author yang
 */
@Getter
@Setter
@Document(collection = "job")
public class Job extends Mongoable implements Pathable {

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
         * Git PR opened event
         */
        PR_OPENED,

        /**
         * Git PR merged event
         */
        PR_MERGED,

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
         * Loading the yaml from git repo
         */
        LOADING,

        /**
         * Job created with yaml and steps
         */
        CREATED,

        /**
         * Been put to job queue
         */
        QUEUED,

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
         * Job been cancelled by user
         */
        CANCELLED,

        /**
         * Job execution time been over the expiredAt
         */
        TIMEOUT
    }

    /**
     * Agent snapshot
     */
    @Getter
    @Setter
    public static class AgentInfo {

        private String name;

        private String os;

        private int cpu;

        private int totalMemory;

        private int freeMemory;

        private int totalDisk;

        private int freeDisk;
    }

    public static Pathable path(Long buildNumber) {
        Job job = new Job();
        job.setBuildNumber(buildNumber);
        return job;
    }

    public final static Set<Status> FINISH_STATUS = ImmutableSet.<Status>builder()
            .add(Status.TIMEOUT)
            .add(Status.CANCELLED)
            .add(Status.FAILURE)
            .add(Status.SUCCESS)
            .build();

    private final static SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final static Integer MinPriority = 1;

    private final static Integer MaxPriority = 255;

    /**
     * Job key is generated from {flow id}-{build number}
     */
    @Indexed(name = "index_job_key", unique = true)
    private String key;

    @Indexed(name = "index_flow_id", sparse = true)
    private String flowId;

    private Long buildNumber;

    private Trigger trigger;

    private Status status = Status.PENDING;

    private Selector agentSelector;

    private String agentId;

    private AgentInfo agentInfo = new AgentInfo();

    private String currentPath;

    private Vars<String> context = new StringVars();

    private String message;

    private Integer priority = MinPriority;

    private boolean isYamlFromRepo;

    private String yamlRepoBranch;

    /**
     * Execution timeout in seconds
     */
    private Long timeout = 1800L;

    /**
     * Expire while queue up
     */
    private Long expire = 1800L;

    /**
     * Total expire from expire and timeout
     */
    private Date expireAt;

    /**
     * Real execution start at
     */
    private Date startAt;

    /**
     * Real execution finish at
     */
    private Date finishAt;

    @JsonIgnore
    public boolean isRunning() {
        return status == Status.RUNNING;
    }

    @JsonIgnore
    public boolean isQueuing() {
        return status == Status.QUEUED;
    }

    @JsonIgnore
    public boolean isDone() {
        return FINISH_STATUS.contains(status);
    }

    @JsonIgnore
    public String getQueueName() {
        return "flow.q." + flowId + ".job";
    }

    @JsonIgnore
    @Override
    public String pathName() {
        return getBuildNumber().toString();
    }

    @JsonIgnore
    public String startAtInStr() {
        if (Objects.isNull(this.startAt)) {
            return StringHelper.EMPTY;
        }
        return DateFormat.format(this.startAt);
    }

    @JsonIgnore
    public String finishAtInStr() {
        if (Objects.isNull(this.startAt)) {
            return StringHelper.EMPTY;
        }
        return DateFormat.format(this.finishAt);
    }

    public String getCredentialName() {
        return context.get(Variables.Flow.GitCredential);
    }

    public String getGitUrl() {
        return context.get(Variables.Flow.GitUrl);
    }

    public void setAgentSnapshot(Agent agent) {
        agentInfo.setName(agent.getName());
        agentInfo.setOs(agent.getOs().name());
        agentInfo.setCpu(agent.getResource().getCpu());
        agentInfo.setTotalMemory(agent.getResource().getTotalMemory());
        agentInfo.setFreeMemory(agent.getResource().getFreeMemory());
        agentInfo.setTotalDisk(agent.getResource().getTotalDisk());
        agentInfo.setFreeDisk(agent.getResource().getFreeDisk());
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
