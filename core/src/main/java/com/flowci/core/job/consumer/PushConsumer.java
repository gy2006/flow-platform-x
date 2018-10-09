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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobPush;
import java.io.IOException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * @author yang
 */
@Log4j2
public abstract class PushConsumer {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private String topicForJobs;

    protected void push(JobPush.Type pushType, Job job) {
        try {
            JobPush push = JobPush.of(pushType, job);
            String json = objectMapper.writeValueAsString(push);
            simpMessagingTemplate.convertAndSend(topicForJobs, json);
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }
}
