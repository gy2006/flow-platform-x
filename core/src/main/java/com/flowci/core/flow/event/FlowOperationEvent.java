/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.flow.event;

import com.flowci.core.flow.domain.Flow;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class FlowOperationEvent extends ApplicationEvent {

    public enum Operation {
        CREATED,

        DELETED
    }

    @Getter
    private final Flow flow;

    @Getter
    private final Operation operation;

    public FlowOperationEvent(Object source, Flow flow, Operation operation) {
        super(source);
        this.flow = flow;
        this.operation = operation;
    }

    public boolean isDeletedEvent() {
        return this.operation == Operation.DELETED;
    }

    public boolean isCreatedEvent() {
        return this.operation == Operation.CREATED;
    }
}
