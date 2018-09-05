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
import com.flowci.domain.Agent;
import com.flowci.domain.ExecutedCmd;
import com.flowci.tree.NodeTree;
import org.springframework.data.domain.Page;

/**
 * @author yang
 */
public interface JobService {

    Job get(Flow flow, Long buildNumber);

    /**
     * List job for flow
     */
    Page<Job> list(Flow flow, int page, int size);

    /**
     * Create job by flow and yml
     */
    Job create(Flow flow, Yml yml, Trigger trigger);

    /**
     * Send to job queue
     */
    Job start(Job job);

    /**
     * Get node tree from job
     */
    NodeTree getTree(Job job);

    /**
     * Job is expired compare to now
     */
    boolean isExpired(Job job);

    /**
     * Convert node to cmd and dispatch to agent
     */
    boolean dispatch(Job job, Agent agent);

    /**
     * Process job from queue
     */
    void processJob(Job job);

    /**
     * Process executed cmd callback from queue
     */
    void processCallback(ExecutedCmd execCmd);
}
