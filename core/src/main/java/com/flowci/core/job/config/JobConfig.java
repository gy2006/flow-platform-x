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

import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.domain.ExecutedCmd;
import com.flowci.tree.NodeTree;
import com.flowci.util.StringHelper;
import com.github.benmanes.caffeine.cache.Cache;
import com.rabbitmq.client.AMQP.Queue;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class JobConfig {

    public final static String LoggingExchange = "cmd.logs";

    @Autowired
    private ConfigProperties appProperties;

    @Autowired
    private ConfigProperties.Job jobProperties;

    @Autowired
    private Channel rabbitChannel;

    @Bean("callbackQueue")
    public String callbackQueue() throws IOException {
        String name = jobProperties.getCallbackQueueName();
        return rabbitChannel.queueDeclare(name, true, false, false, null).getQueue();
    }

    @Bean("loggingQueue")
    public String loggingQueue() throws IOException {
        DeclareOk loggingQueue = rabbitChannel.queueDeclare();
        rabbitChannel.exchangeDeclare(LoggingExchange, BuiltinExchangeType.FANOUT);
        rabbitChannel.queueBind(loggingQueue.getQueue(), LoggingExchange, StringHelper.EMPTY);
        return loggingQueue.getQueue();
    }

    @Bean("logDir")
    public Path logDir() {
        return appProperties.getLogDir();
    }

    @Bean("jobDeleteExecutor")
    public ThreadPoolTaskExecutor jobDeleteExecutor() {
        return ThreadHelper.createTaskExecutor(1, 1, 10, "job-delete-");
    }

    @Bean("jobLogExecutor")
    public ThreadPoolTaskExecutor jobLogExecutor() {
        return ThreadHelper.createTaskExecutor(1, 1, 1, "job-logging-");
    }

    @Bean("jobConsumerExecutor")
    public ThreadPoolTaskExecutor jobConsumerExecutor() {
        return ThreadHelper.createTaskExecutor(100, 100, 0, "job-consumer-");
    }

    @Bean("jobTreeCache")
    public Cache<String, NodeTree> jobTreeCache() {
        return CacheHelper.createLocalCache(50, 60);
    }

    @Bean("jobStepCache")
    public Cache<String, List<ExecutedCmd>> jobStepCache() {
        return CacheHelper.createLocalCache(100, 60);
    }
}
