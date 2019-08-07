/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.auth.config;

import com.flowci.core.auth.domain.PermissionMap;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.credential.domain.CredentialAction;
import com.flowci.core.flow.domain.FlowAction;
import com.flowci.core.job.domain.JobAction;
import com.flowci.core.user.domain.User;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import java.text.MessageFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yang
 */
@Configuration
public class AuthConfig {

    public static final String CACHE_ONLINE = "online_users";

    @Autowired
    private ConfigProperties.Auth authProperties;

    @Bean
    public ThreadLocal<User> currentUser() {
        return new ThreadLocal<>();
    }

    @Bean
    public CacheManager authCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_ONLINE);

        String spec = MessageFormat.format(
            "maximumSize={0},expireAfterWrite={1}s",
            authProperties.getMaxUsers(),
            authProperties.getExpireSeconds()
        );
        cacheManager.setCaffeineSpec(CaffeineSpec.parse(spec));
        return cacheManager;
    }

    @Bean
    public PermissionMap actionMap() {
        PermissionMap permissionMap = new PermissionMap();

        // admin
        permissionMap.add(User.Role.Admin, FlowAction.ALL);
        permissionMap.add(User.Role.Admin, JobAction.ALL);
        permissionMap.add(User.Role.Admin, CredentialAction.ALL);

        // developer
        permissionMap.add(User.Role.Developer,
                FlowAction.GET, FlowAction.LIST, FlowAction.LIST_BRANCH, FlowAction.GET, FlowAction.GET_YML);
        permissionMap.add(User.Role.Developer, JobAction.ALL);
        permissionMap.add(User.Role.Developer, CredentialAction.LIST_NAME);

        return permissionMap;
    }
}
