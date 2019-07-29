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

import com.flowci.domain.Jsonable;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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

    private static final String LoggingExchange = "cmd.logs";

    public static final Jackson2JsonMessageConverter JACKSON_2_JSON_MESSAGE_CONVERTER =
        new Jackson2JsonMessageConverter(Jsonable.getMapper());

    @Autowired
    private ConfigProperties.Job jobProperties;

    @Bean("queueAdmin")
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean("callbackQueue")
    public Queue callbackQueue() {
        String callbackQueue = jobProperties.getCallbackQueueName();
        return QueueBuilder.durable(callbackQueue).build();
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

    @Bean("queueTemplate")
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(JACKSON_2_JSON_MESSAGE_CONVERTER);
        return template;
    }

    @Bean("callbackQueueContainerFactory")
    public SimpleRabbitListenerContainerFactory callbackQueueContainerFactory(ConnectionFactory connectionFactory) {
        return createContainerFactory(connectionFactory, 1, QueueConfig.JACKSON_2_JSON_MESSAGE_CONVERTER);
    }

    @Bean("logsContainerFactory")
    public SimpleRabbitListenerContainerFactory logsContainerFactory(ConnectionFactory connectionFactory) {
        return createContainerFactory(connectionFactory, 1, null);
    }

    private SimpleRabbitListenerContainerFactory createContainerFactory(ConnectionFactory connectionFactory,
                                                                        int concurrent,
                                                                        MessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrentConsumers(concurrent);
        factory.setMaxConcurrentConsumers(concurrent);
        factory.setMessageConverter(converter);
        return factory;
    }
}
