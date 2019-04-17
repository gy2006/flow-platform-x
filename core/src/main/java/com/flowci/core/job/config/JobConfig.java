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

import com.flowci.core.config.ConfigProperties;
import com.flowci.core.helper.CacheHelper;
import com.flowci.core.helper.ThreadHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class JobConfig {

    @Autowired
    private ConfigProperties appProperties;

    @Bean("logDir")
    public Path logDir() {
        return appProperties.getLogDir();
    }

    @Bean("retryExecutor")
    public ThreadPoolTaskExecutor retryExecutor() {
        return ThreadHelper.createTaskExecutor(1, 1, 100, "job-retry-");
    }

    /**
     * Thread to record incoming logs
     */
    @Bean("logsExecutor")
    public ThreadPoolTaskExecutor logsExecutor(){
        return ThreadHelper.createTaskExecutor(10, 10, 1000, "log-writer-");
    }

    @Primary
    @Bean("jobCacheManager")
    public CacheManager cacheManager() {
        return CacheHelper.createLocalCacheManager(100, 120);
    }

    @Bean("logCache")
    public com.github.benmanes.caffeine.cache.Cache<String, BufferedWriter> logCacheManager() {
        return CacheHelper.createLocalCache(10, 600, (key, value, cause) -> {
            if (Objects.isNull(value)) {
                return;
            }

            try {
                value.close();
            } catch (IOException e) {
                log.debug(e);
            }
        });
    }

    @Bean("jobTreeCache")
    public Cache jobTreeCache(CacheManager jobCacheManager) {
        return jobCacheManager.getCache("JOB_TREE");
    }

    @Bean("jobStepCache")
    public Cache jobStepCache(CacheManager jobCacheManager) {
        return jobCacheManager.getCache("JOB_STEPS");
    }
}
