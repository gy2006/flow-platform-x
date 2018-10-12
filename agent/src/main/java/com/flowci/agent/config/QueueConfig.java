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

import com.flowci.agent.service.CmdService;
import com.flowci.domain.Jsonable;
import com.flowci.domain.Settings;
import com.flowci.domain.Settings.RabbitMQ;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Configuration
@Order(2)
public class QueueConfig {

    private final Jackson2JsonMessageConverter queueMessageConverter =
        new Jackson2JsonMessageConverter(Jsonable.getMapper());

    @Autowired
    private Settings agentSettings;

    @Bean("agentQueue")
    public Queue jobQueue() {
        String agentQueue = agentSettings.getAgent().getQueueName();
        return new Queue(agentQueue, true);
    }

    @Bean("callbackQueue")
    public Queue callbackQueue() {
        String queueName = agentSettings.getCallbackQueueName();
        return new Queue(queueName, true);
    }

    @Bean("logsExchange")
    public String logsExchange() {
        return agentSettings.getLogsExchangeName();
    }

    @Bean
    public ConnectionFactory factory() {
        RabbitMQ mq = agentSettings.getQueue();
        CachingConnectionFactory factory = new CachingConnectionFactory(mq.getHost(), mq.getPort());
        factory.setUsername(mq.getUsername());
        factory.setPassword(mq.getPassword());
        return factory;
    }

    @Bean
    public MessageListenerAdapter adapter(CmdService cmdService) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(cmdService, "onCmdReceived");
        adapter.setMessageConverter(queueMessageConverter);
        return adapter;
    }

    @Bean
    public ThreadPoolTaskExecutor consumerExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.setQueueCapacity(0);
        taskExecutor.setThreadNamePrefix("cmd-consumer-");
        taskExecutor.setDaemon(true);
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean
    public SimpleMessageListenerContainer container(ConnectionFactory factory,
                                                    MessageListenerAdapter adapter,
                                                    ThreadPoolTaskExecutor consumerExecutor) {
        String queueName = agentSettings.getAgent().getQueueName();

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.setQueueNames(queueName);
        container.setMessageListener(adapter);
        container.setTaskExecutor(consumerExecutor);
        return container;
    }

    @Bean("queueTemplate")
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(queueMessageConverter);
        return template;
    }
}
