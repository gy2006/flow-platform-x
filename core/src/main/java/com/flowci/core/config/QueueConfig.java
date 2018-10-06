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

package com.flowci.core.config;

import com.flowci.domain.Jsonable;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yang
 */
@Log4j2
@Configuration
@EnableRabbit
public class QueueConfig {

    private static final String LoggingExchange = "flowci-logging";

    private final Jackson2JsonMessageConverter queueMessageConverter =
        new Jackson2JsonMessageConverter(Jsonable.getMapper());

    @Autowired
    private ConfigProperties.Job jobProperties;

    @Bean("queueAdmin")
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean("jobQueue")
    public Queue jobQueue() {
        String jobQueue = jobProperties.getQueueName();
        return new Queue(jobQueue, true);
    }

    @Bean("callbackQueue")
    public Queue callbackQueue() {
        String callbackQueue = jobProperties.getCallbackQueueName();
        return new Queue(callbackQueue, true);
    }

    @Bean("logsExchange")
    public FanoutExchange logsExchange(RabbitAdmin queueAdmin) {
        FanoutExchange exchange = new FanoutExchange(LoggingExchange, false, true);
        queueAdmin.declareExchange(exchange);
        return exchange;
    }

    @Bean("logsQueue")
    public Queue logsQueue(RabbitAdmin queueAdmin) {
        return queueAdmin.declareQueue();
    }

    @Bean("logsBinding")
    public Binding logsBinding(RabbitAdmin queueAdmin, Queue logsQueue, FanoutExchange logsExchange) {
        Binding binding = BindingBuilder.bind(logsQueue).to(logsExchange);
        queueAdmin.declareBinding(binding);
        return binding;
    }

    @Bean("jobAndCallbackContainerFactory")
    public SimpleRabbitListenerContainerFactory jobAndCallbackContainerFactory(ConnectionFactory connectionFactory) {
        return createContainerFactory(connectionFactory, 1);
    }

    @Bean("logsContainerFactory")
    public SimpleRabbitListenerContainerFactory logsContainerFactory(ConnectionFactory connectionFactory) {
        return createContainerFactory(connectionFactory, 5);
    }

    @Bean("queueTemplate")
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(queueMessageConverter);
        return template;
    }

    private SimpleRabbitListenerContainerFactory createContainerFactory(ConnectionFactory connectionFactory,
                                                                        int concurrent) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(concurrent);
        factory.setMaxConcurrentConsumers(concurrent);
        factory.setMessageConverter(queueMessageConverter);
        return factory;
    }
}
