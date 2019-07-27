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

import com.flowci.core.common.domain.PushEvent;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.StepStatusChangeEvent;
import com.flowci.core.message.PushService;
import com.flowci.domain.ExecutedCmd;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class OnStepStatusChange implements ApplicationListener<StepStatusChangeEvent> {

    @Autowired
    private String topicForSteps;

    @Autowired
    private PushService pushService;

    @Override
    public void onApplicationEvent(StepStatusChangeEvent event) {
        Job job = event.getJob();
        ExecutedCmd cmd = event.getExecutedCmd();

        String topic = topicForSteps + "/" + job.getId();
        pushService.push(topic, PushEvent.STATUS_CHANGE, cmd);

        log.debug("Executed cmd {} with status {} been pushed to topic {}", cmd.getId(), cmd.getStatus(), topic);
    }
}
