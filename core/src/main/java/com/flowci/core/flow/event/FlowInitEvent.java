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

import java.util.List;

public class FlowInitEvent extends ApplicationEvent {

    @Getter
    private final List<Flow> flows;

    public FlowInitEvent(Object source, List<Flow> flows) {
        super(source);
        this.flows = flows;
    }
}
