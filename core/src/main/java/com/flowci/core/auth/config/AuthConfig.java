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

import com.flowci.core.agent.domain.AgentAction;
import com.flowci.core.agent.domain.AgentHostAction;
import com.flowci.core.auth.domain.PermissionMap;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.secret.domain.SecretAction;
import com.flowci.core.flow.domain.FlowAction;
import com.flowci.core.job.domain.JobAction;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.domain.UserAction;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author yang
 */
@Configuration
public class AuthConfig {

    private static final String CACHE_ONLINE = "online_users";

    private static final String CACHE_REFRESH_TOKEN = "refresh_tokens";

    private static final long MaxCacheSize = 500;

    @Autowired
    private ConfigProperties.Auth authProperties;

    @Bean
    public Cache onlineUsersCache() {
        return new CaffeineCache(CACHE_ONLINE,
                Caffeine.newBuilder()
                        .maximumSize(MaxCacheSize)
                        .expireAfterWrite(authProperties.getExpireSeconds(), TimeUnit.SECONDS)
                        .build());
    }

    @Bean
    public Cache refreshTokenCache() {
        return new CaffeineCache(CACHE_REFRESH_TOKEN,
                Caffeine.newBuilder()
                        .maximumSize(MaxCacheSize)
                        .expireAfterWrite(authProperties.getRefreshExpiredSeconds(), TimeUnit.SECONDS)
                        .build());
    }

    @Bean
    public PermissionMap actionMap() {
        PermissionMap permissionMap = new PermissionMap();

        // admin
        permissionMap.add(User.Role.Admin, FlowAction.ALL);
        permissionMap.add(User.Role.Admin, JobAction.ALL);
        permissionMap.add(User.Role.Admin, SecretAction.ALL);
        permissionMap.add(User.Role.Admin, AgentAction.ALL);
        permissionMap.add(User.Role.Admin, AgentHostAction.ALL);
        permissionMap.add(User.Role.Admin, UserAction.ALL);

        // developer
        permissionMap.add(User.Role.Developer,
                FlowAction.GET, FlowAction.LIST, FlowAction.LIST_BRANCH, FlowAction.GET, FlowAction.GET_YML);
        permissionMap.add(User.Role.Developer, JobAction.ALL);
        permissionMap.add(User.Role.Developer, SecretAction.LIST_NAME);
        permissionMap.add(User.Role.Developer, AgentAction.GET, AgentAction.LIST);
        permissionMap.add(User.Role.Developer, AgentHostAction.GET, AgentHostAction.LIST);
        permissionMap.add(User.Role.Developer, UserAction.CHANGE_PASSWORD, UserAction.UPDATE_AVATAR);

        return permissionMap;
    }
}
