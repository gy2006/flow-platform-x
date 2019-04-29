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
import com.flowci.domain.ExecutedCmd;
import com.flowci.tree.NodeTree;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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

    @Bean("logWriterCache")
    public Cache<String, BufferedWriter> logWriterCache() {
        return CacheHelper.createLocalCache(10, 600, new CloseWriterAndReader<>());
    }

    @Bean("logReaderCache")
    public Cache<String, BufferedReader> logReaderCache() {
        return CacheHelper.createLocalCache(10, 60, new CloseWriterAndReader<>());
    }

    @Bean("jobTreeCache")
    public Cache<String, NodeTree> jobTreeCache() {
        return CacheHelper.createLocalCache(50, 60);
    }

    @Bean("jobStepCache")
    public Cache<String, List<ExecutedCmd>> jobStepCache() {
        return CacheHelper.createLocalCache(100, 60);
    }

    private class CloseWriterAndReader<K, V extends Closeable> implements RemovalListener<K, V> {

        @Override
        public void onRemoval(K key, V value, RemovalCause cause) {
            if (Objects.isNull(value)) {
                return;
            }

            try {
                value.close();
            } catch (IOException e) {
                log.debug(e);
            }
        }
    }
}
