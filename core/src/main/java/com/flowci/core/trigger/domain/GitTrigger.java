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

package com.flowci.core.trigger.domain;

import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.domain.VariableMap;
import com.flowci.exception.NotFoundException;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@Getter
@Setter
@ToString(of = {"source", "event"})
public abstract class GitTrigger implements Serializable {

    private GitSource source;

    private GitEvent event;

    public enum GitSource {

        GITLAB,

        GITHUB,

        CODING,

        OSCHINA,

        BITBUCKET
    }

    public enum GitEvent {

        PING,

        PUSH,

        PR_OPEN,

        PR_CLOSE,

        TAG
    }

    public VariableMap toVariableMap() {
        VariableMap map = new VariableMap(15);
        map.put(Variables.GIT_SOURCE, source.name());
        map.put(Variables.GIT_EVENT, event.name());
        return map;
    }

    /**
     * Convert git trigger to job trigger
     */
    public Trigger toJobTrigger() {
        if (event == GitEvent.PUSH) {
            return Trigger.PUSH;
        }

        if (event == GitEvent.TAG) {
            return Trigger.TAG;
        }

        if (event == GitEvent.PR_OPEN) {
            return Trigger.PR_OPEN;
        }

        if (event == GitEvent.PR_CLOSE) {
            return Trigger.PR_CLOSE;
        }

        throw new NotFoundException("Cannot found related job trigger for {0}", event.name());
    }
}
