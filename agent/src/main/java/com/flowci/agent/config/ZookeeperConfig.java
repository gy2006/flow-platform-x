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

package com.flowci.agent.config;

import com.flowci.domain.Settings;
import com.flowci.zookeeper.ZookeeperClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Configuration
@Order(3)
public class ZookeeperConfig {

    @Autowired
    private Settings agentSettings;

    @Bean
    public ThreadPoolTaskExecutor zkWatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setDaemon(true);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("zk-watch-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "zk")
    public ZookeeperClient zkClient(ThreadPoolTaskExecutor zkWatchExecutor) {
        String host = agentSettings.getZookeeper().getHost();
        ZookeeperClient client = new ZookeeperClient(host, 5, 30, zkWatchExecutor);
        client.start();
        return client;
    }
}
