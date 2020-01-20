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

package com.flowci.pool.domain;

import java.io.Serializable;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@Setter
public abstract class PoolContext implements Serializable {

    public static final String ContainerNamePerfix = "ci-agent-";

    public static class AgentEnvs {

        public static final String SERVER_URL = "FLOWCI_SERVER_URL";

        public static final String AGENT_TOKEN = "FLOWCI_AGENT_TOKEN";

        public static final String AGENT_PORT = "FLOWCI_AGENT_PORT";

        public static final String AGENT_LOG_LEVEL = "FLOWCI_AGENT_LOG_LEVEL";

    }

	public static abstract class DockerStatus {

        public static final String None = "none";

		public static final String Created = "created";

		public static final String Restarting = "restarting";

		public static final String Running = "running";

		public static final String Removing = "removing";

		public static final String Paused = "paused";

		public static final String Exited = "exited";

		public static final String Dead = "dead";
	}

    @NonNull
    private String serverUrl; // ex: http://127.0.0.1:8080

    @NonNull
    private String token;

    private Integer port = 8088;

    @NonNull
    private String logLevel = "DEBUG";

    public String getContainerName() {
        return String.format("%s%s", ContainerNamePerfix, token);
    }

    public String getDirOnHost() {
        return String.format("${HOME}/.flow.agent-%s", token);
    }
}
