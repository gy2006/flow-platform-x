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

package com.flowci.core.job.consumer;

import com.flowci.core.domain.PushEvent;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobStatusChangeEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Push job status change event via WebSocket
 *
 * @author yang
 */
@Log4j2
@Component
public class JobStatusChangeConsumer extends PushConsumer<Job> implements ApplicationListener<JobStatusChangeEvent> {

    @Autowired
    private String topicForJobs;

    @Override
    public void onApplicationEvent(JobStatusChangeEvent event) {
        Job job = event.getJob();
        push(topicForJobs, PushEvent.STATUS_CHANGE, job);
        log.debug("Job {} with status {} been pushed", job.getId(), job.getStatus());
    }
}
