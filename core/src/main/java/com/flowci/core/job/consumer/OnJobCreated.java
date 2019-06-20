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
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.message.PushService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class OnJobCreated implements ApplicationListener<JobCreatedEvent> {

    @Autowired
    private String topicForJobs;

    @Autowired
    private PushService pushService;

    @Override
    public void onApplicationEvent(JobCreatedEvent event) {
        Job job = event.getJob();
        pushService.push(topicForJobs, PushEvent.NEW_CREATED, job);
        log.debug("Job {} been pushed", job.getId());
    }
}
