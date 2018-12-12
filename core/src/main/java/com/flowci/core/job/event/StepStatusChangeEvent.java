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

package com.flowci.core.job.event;

import com.flowci.core.job.domain.Job;
import com.flowci.domain.ExecutedCmd;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * @author yang
 */
public class StepStatusChangeEvent extends ApplicationEvent {

    @Getter
    private final Job job;

    @Getter
    private final ExecutedCmd executedCmd;

    public StepStatusChangeEvent(Object source, Job job, ExecutedCmd executedCmd) {
        super(source);
        this.job = job;
        this.executedCmd = executedCmd;
    }
}
