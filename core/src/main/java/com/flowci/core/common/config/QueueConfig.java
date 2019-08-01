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

package com.flowci.core.common.config;

import com.flowci.core.common.manager.RabbitChannelManager;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.manager.RabbitQueueManager;
import com.flowci.util.StringHelper;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
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
public class QueueConfig {

    public final static String LoggingExchange = "cmd.logs";

    @Autowired
    private ConfigProperties.RabbitMQ rabbitProperties;

    @Autowired
    private ConfigProperties.Job jobProperties;

    @Bean
    public ThreadPoolTaskExecutor rabbitConsumerExecutor() {
        return ThreadHelper.createTaskExecutor(20, 20, 50, "rabbit-t-");
    }

    @Bean
    public Connection rabbitConnection(ThreadPoolTaskExecutor rabbitConsumerExecutor)
        throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(rabbitProperties.getUsername());
        factory.setPassword(rabbitProperties.getPassword());
        factory.setVirtualHost("/");
        factory.setHost(rabbitProperties.getHost());
        factory.setPort(rabbitProperties.getPort());
        factory.setRequestedHeartbeat(1800);

        return factory.newConnection(rabbitConsumerExecutor.getThreadPoolExecutor());
    }

    @Bean
    public RabbitQueueManager callbackQueueManager(Connection rabbitConnection) throws IOException {
        String name = jobProperties.getCallbackQueueName();
        RabbitQueueManager manager = new RabbitQueueManager(rabbitConnection, 1, name);
        manager.declare(true);
        return manager;
    }

    @Bean
    public RabbitQueueManager loggingQueueManager(Connection rabbitConnection) throws IOException {
        String name = "queue.logging." + UUID.randomUUID();
        RabbitQueueManager manager = new RabbitQueueManager(rabbitConnection, 1, name);
        manager.declare(false);

        Channel channel = manager.getChannel();
        channel.exchangeDeclare(LoggingExchange, BuiltinExchangeType.FANOUT);
        channel.queueBind(manager.getQueueName(), LoggingExchange, StringHelper.EMPTY);

        return manager;
    }

    @Bean
    public RabbitChannelManager agentQueueManager(Connection rabbitConnection) throws IOException {
        return new RabbitChannelManager(rabbitConnection, 1, "agent-channel-mgr");
    }
}
