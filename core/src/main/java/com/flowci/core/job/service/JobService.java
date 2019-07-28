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

package com.flowci.core.job.service;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobYml;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.VariableMap;
import org.springframework.data.domain.Page;

/**
 * @author yang
 */
public interface JobService {

    /**
     * Get job by id
     */
    Job get(String id);

    /**
     * Get job by flow and build number
     */
    Job get(Flow flow, Long buildNumber);

    /**
     * Get job yml by job
     */
    JobYml getYml(Job job);

    /**
     * Get latest job
     */
    Job getLatest(Flow flow);

    /**
     * List job for flow
     */
    Page<Job> list(Flow flow, int page, int size);

    /**
     * Create job by flow and yml
     */
    Job create(Flow flow, Yml yml, Trigger trigger, VariableMap input);

    /**
     * Send to job queue
     */
    Job start(Job job);

    /**
     * Force to stop the job if it's running
     */
    Job cancel(Job job);

    /**
     * Delete all jobs of the flow within an executor
     */
    void delete(Flow flow);

    /**
     * Job is expired compare to now
     */
    boolean isExpired(Job job);

    /**
     * Handle the job from queue
     */
    void handleJob(Job job);

    /**
     * Process executed cmd callback from queue
     */
    void handleCallback(ExecutedCmd execCmd);
}
