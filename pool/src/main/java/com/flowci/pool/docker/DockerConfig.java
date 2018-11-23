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

package com.flowci.pool.docker;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig.Builder;
import com.github.dockerjava.core.DockerClientConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@Setter
@RequiredArgsConstructor(staticName = "of")
public final class DockerConfig {

    @Getter
    @Setter
    public static class RegistryConfig {

        private String url;

        private String email;

        private String username;

        private String password;
    }

    private final String host;

    private RegistryConfig registry;

    public DockerClientConfig create() {
        Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost(host);

        if (registry != null) {
            builder = builder.withRegistryUrl(registry.url)
                .withRegistryUsername(registry.username)
                .withRegistryPassword(registry.password)
                .withRegistryEmail(registry.email);
        }

        return builder.build();
    }
}
