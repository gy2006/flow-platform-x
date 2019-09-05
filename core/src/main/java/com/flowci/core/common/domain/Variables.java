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

package com.flowci.core.common.domain;

/**
 * @author yang
 */
public class Variables {

    public static class App {

        public static final String Url = "FLOWCI_SERVER_URL";

        public static final String Host = "FLOWCI_SERVER_HOST";

        public static final String RabbitHost = "FLOWCI_RABBIT_HOST";

        public static final String ZookeeperHost = "FLOWCI_ZOOKEEPER_HOST";

    }

    public static class Flow {

        public static final String Name = "FLOWCI_FLOW_NAME";

        public static final String Webhook = "FLOWCI_FLOW_WEBHOOK";

        public static final String GitUrl = "FLOWCI_GIT_URL";

        public static final String GitBranch = "FLOWCI_GIT_BRANCH";

        public static final String SSH_RSA = "FLOWCI_CREDENTIAL_SSH_RSA";

        // which define the flow work dir which under the agent default workspace
        public static final String WorkDir = "FLOWCI_FLOW_WORKDIR";
    }

    public static class Job {

        public static final String BuildNumber = "FLOWCI_JOB_BUILD_NUM";

        public static final String Status = "FLOWCI_JOB_STATUS";

        public static final String Trigger = "FLOWCI_JOB_TRIGGER";

    }

    private Variables() {
    }
}
