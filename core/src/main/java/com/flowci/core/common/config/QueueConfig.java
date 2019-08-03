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

import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.rabbit.RabbitChannelOperation;
import com.flowci.core.common.rabbit.RabbitQueueOperation;
import com.flowci.util.StringHelper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class QueueConfig {

    public final static String LoggingExchange = "cmd.logs";

    @Autowired
    private ConfigProperties.RabbitMQ rabbitProperties;

    @Bean
    public ThreadPoolTaskExecutor rabbitConsumerExecutor() {
        return ThreadHelper.createTaskExecutor(10, 10, 50, "rabbit-t-");
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

        Connection connection = factory.newConnection(rabbitConsumerExecutor.getThreadPoolExecutor());
        connection.addShutdownListener(cause -> log.error(cause));
        return connection;
    }

    @Bean
    public RabbitQueueOperation callbackQueueManager(Connection rabbitConnection) throws IOException {
        String name = rabbitProperties.getCallbackQueueName();
        RabbitQueueOperation manager = new RabbitQueueOperation(rabbitConnection, 10, name);
        manager.declare(true);
        return manager;
    }

    @Bean
    public RabbitQueueOperation loggingQueueManager(Connection rabbitConnection) throws IOException {
        String name = rabbitProperties.getLoggingQueueName();
        RabbitQueueOperation manager = new RabbitQueueOperation(rabbitConnection, 10, name);
        manager.declare(false);

        Channel channel = manager.getChannel();
        channel.exchangeDeclare(LoggingExchange, BuiltinExchangeType.FANOUT);
        channel.queueBind(manager.getQueueName(), LoggingExchange, StringHelper.EMPTY);

        return manager;
    }

    @Bean
    public RabbitChannelOperation agentQueueManager(Connection rabbitConnection) throws IOException {
        return new RabbitChannelOperation(rabbitConnection, 1, "agent-channel-mgr");
    }
}
