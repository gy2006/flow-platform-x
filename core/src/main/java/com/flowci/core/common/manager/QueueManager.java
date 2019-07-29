/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.common.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.util.StringHelper;
import com.google.common.base.Strings;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class QueueManager {

    @Autowired
    private Connection rabbitConnection;

    @Autowired
    private Channel rabbitChannel;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, String> consumerTags = new ConcurrentHashMap<>();

    @EventListener
    public void cleanUp(ContextStoppedEvent event) {
        try {
            log.debug("RabbitMQ connection will be closed");
            rabbitChannel.close();
            rabbitConnection.close();
        } catch (IOException | TimeoutException e) {
            log.warn(e);
        }
    }

    public boolean declare(String queue, boolean durable) {
        try {
            rabbitChannel.queueDeclare(queue, durable, false, false, null);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean declare(String queue, boolean durable, Integer maxPriority) {
        try {
            Map<String, Object> props = new HashMap<>(1);
            props.put("x-max-priority", maxPriority);

            rabbitChannel.queueDeclare(queue, durable, false, false, props);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean delete(String queue) {
        try {
            rabbitChannel.queueDelete(queue);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean purge(String queue) {
        try {
            rabbitChannel.queuePurge(queue);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Send to routing key with default exchange
     */
    public boolean send(String routingKey, Object content) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(content);
            rabbitChannel.basicPublish(StringHelper.EMPTY, routingKey, null, bytes);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Send to routing key with default exchange and priority
     */
    public boolean send(String routingKey, Object content, Integer priority) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(content);

            BasicProperties.Builder basicProps = new BasicProperties.Builder();
            basicProps.priority(priority);

            rabbitChannel.basicPublish(StringHelper.EMPTY, routingKey, basicProps.build(), bytes);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean startListen(String queue, Consumer<String> consumer) {
        try {
            String consumerTag = rabbitChannel.basicConsume(queue, false, new DefaultConsumer(rabbitChannel) {
                @Override
                public void handleDelivery(String consumerTag,
                                           Envelope envelope,
                                           BasicProperties properties,
                                           byte[] body) throws IOException {

                    try {
                        String raw = new String(body, StandardCharsets.UTF_8);
                        consumer.accept(raw);
                    } catch (Throwable e) {
                        log.warn(e);
                    } finally {
                        rabbitChannel.basicAck(envelope.getDeliveryTag(), false);
                    }
                }
            });

            consumerTags.put(queue, consumerTag);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean stopListen(String queue) {
        String consumerTag = consumerTags.get(queue);

        if (Strings.isNullOrEmpty(consumerTag)) {
            return true;
        }

        try {
            rabbitChannel.basicCancel(consumerTag);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
