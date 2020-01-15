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

package com.flowci.pool;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@Setter
public abstract class PoolContext implements Serializable {

    public static class AgentEnvs {

        public static final String SERVER_URL = "FLOWCI_SERVER_URL";

        public static final String AGENT_TOKEN = "FLOWCI_AGENT_TOKEN";

        public static final String AGENT_PORT = "FLOWCI_AGENT_PORT";

        public static final String AGENT_LOG_LEVEL = "FLOWCI_AGENT_LOG_LEVEL";

    }

    private URI serverUrl;

    private String token;

    private Integer port = 8088;

    private String logLevel = "DEBUG";

    private String status;

    private Date startAt;
}
