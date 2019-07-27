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

package com.flowci.core.common.config;

import java.nio.file.Path;
import lombok.Data;

/**
 * @author yang
 */
@Data
public class ConfigProperties {

    private Path workspace;

    private Path logDir;

    private Boolean authEnabled;

    private String serverAddress;

    @Data
    public static class Admin {

        private String defaultEmail;

        private String defaultPassword;
    }

    @Data
    public static class Job {

        private String queueName;

        private String callbackQueueName;

        private Long expireInSeconds;

        private Long retryWaitingSeconds;
    }

    @Data
    public static class Plugin {

        private String defaultRepo;

        private Boolean autoUpdate;
    }


    @Data
    public static class Zookeeper {

        private Boolean embedded;

        private String host;

        private String agentRoot;

        private String cronRoot;

        private Integer timeout;

        private Integer retry;

        private String dataDir;
    }
}
