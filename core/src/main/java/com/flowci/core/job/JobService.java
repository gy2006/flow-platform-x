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

package com.flowci.core.job;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.domain.Agent;

/**
 * @author yang
 */
public interface JobService {

    /**
     * Start a job and send to queue
     */
    Job start(Flow flow, Yml yml, Trigger trigger);

    /**
     * Job is expired compare to now
     */
    boolean isExpired(Job job);

    /**
     * Process job from queue
     */
    void processJob(Job job);

    /**
     * Dispatch job to selected agent
     */
    boolean dispatch(Job job, Agent agent);
}
