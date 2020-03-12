/*
 * Copyright 2020 flow.ci
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

import com.flowci.domain.ExecutedCmd;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class StepInitializedEvent extends ApplicationEvent {

    private final String jobId;

    private final List<ExecutedCmd> steps;

    public StepInitializedEvent(Object source, String jobId, List<ExecutedCmd> steps) {
        super(source);
        this.jobId = jobId;
        this.steps = steps;
    }
}
