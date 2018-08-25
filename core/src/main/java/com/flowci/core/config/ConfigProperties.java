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

package com.flowci.core.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yang
 */
@Data
public class ConfigProperties {

    private String workspace;

    private final Admin admin = new Admin();

    @Data
    public static class Admin {

        private String email;

        private String password;
    }

    @Data
    public static class Job {

        private String queueName;

        private Integer expireInSeconds;
    }


    @Data
    public static class Zookeeper {

        private Boolean embedded;

        private String host;

        private String root;

        private Integer timeout;

        private Integer retry;

        private String dataDir;
    }
}
