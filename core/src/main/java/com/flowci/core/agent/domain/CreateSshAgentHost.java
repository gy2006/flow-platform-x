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

package com.flowci.core.agent.domain;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public class CreateSshAgentHost {

    @NotEmpty
    private String name;

    @NotEmpty
    private String credential;

    @NotEmpty
    private String user;

    @NotEmpty
    private String ip;

    private int maxSize = 10;

    private int maxIdleSeconds = 3600;

    private int maxOfflineSeconds = 600;

    public SshAgentHost toObj() {
        SshAgentHost host = new SshAgentHost();
        host.setName(name);
        host.setCredential(credential);
        host.setUser(user);
        host.setIp(ip);
        host.setMaxSize(maxSize);
        host.setMaxIdleSeconds(maxIdleSeconds);
        host.setMaxOfflineSeconds(maxOfflineSeconds);
        return host;
    }
}
