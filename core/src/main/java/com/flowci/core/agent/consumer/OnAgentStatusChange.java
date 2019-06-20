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

package com.flowci.core.agent.consumer;

import com.flowci.core.agent.event.StatusChangeEvent;
import com.flowci.core.domain.PushEvent;
import com.flowci.core.message.PushService;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class OnAgentStatusChange implements ApplicationListener<StatusChangeEvent> {

    @Autowired
    private PushService pushService;

    @Autowired
    private String topicForAgents;

    @Override
    public void onApplicationEvent(StatusChangeEvent event) {
        Agent agent = event.getAgent();
        pushService.push(topicForAgents, PushEvent.STATUS_CHANGE, agent);

        String name = agent.getName();
        Status status = agent.getStatus();
        String jobId = agent.getJobId();
        log.debug("Agent {} with status {} for job {} been pushed", name, status, jobId);
    }
}
