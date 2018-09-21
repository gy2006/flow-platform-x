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

import com.flowci.domain.VariableMap;
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

        PUSH,

        PR_OPEN,

        PR_CLOSE,

        TAG
    }

    public static class Variables {

        /**
         * Git event source
         */
        public static final String GIT_SOURCE = "FLOWCI_GIT_SOURCE";

        /**
         * Git event type
         */
        public static final String GIT_EVENT = "FLOWCI_GIT_EVENT";

        /**
         * Should be email of the git user who start an event
         */
        public static final String GIT_AUTHOR = "FLOWCI_GIT_AUTHOR";

    }

    public VariableMap toVariableMap() {
        VariableMap map = new VariableMap(15);
        map.putString(Variables.GIT_SOURCE, source.name());
        map.putString(Variables.GIT_EVENT, event.name());
        return map;
    }
}
