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

package com.flowci.core.job.config;

import com.flowci.core.helper.ThreadHelper;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Configuration
public class JobConfig {

    @Bean("retryExecutor")
    public ThreadPoolTaskExecutor retryExecutor() {
        return ThreadHelper.createTaskExecutor(1, 1, 100, "job-retry-");
    }

    @Bean("jobCacheManager")
    public CacheManager cacheManager() {
        Caffeine<Object, Object> cache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(30, TimeUnit.SECONDS);

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(cache);
        return cacheManager;
    }

    @Bean("jobTreeCache")
    public Cache jobTreeCache(CacheManager jobCacheManager) {
        return jobCacheManager.getCache("JOB_TREE");
    }
}
