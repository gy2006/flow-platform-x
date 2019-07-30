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

package com.flowci.core.common.helper;

import com.flowci.util.StringHelper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

/**
 * @author yang
 */
@Getter
public final class RabbitBuilder implements AutoCloseable {

    private final Connection conn;

    private final Channel channel;

    private final String name;

    private final Integer concurrency;

    private final ConcurrentHashMap<String, String> tags = new ConcurrentHashMap<>();

    public RabbitBuilder(Connection conn, Integer concurrency, String name) throws IOException {
        this.conn = conn;
        this.name = name;
        this.concurrency = concurrency;

        this.channel = conn.createChannel();
        this.channel.basicQos(0, concurrency, false);
    }

    public String declare(String queue, boolean durable) {
        try {
            return this.channel.queueDeclare(queue, durable, false, false, null).getQueue();
        } catch (IOException e) {
            return null;
        }
    }

    public String declare(String queue, boolean durable, Integer maxPriority) {
        try {
            Map<String, Object> props = new HashMap<>(1);
            props.put("x-max-priority", maxPriority);

            return this.channel.queueDeclare(queue, durable, false, false, props).getQueue();
        } catch (IOException e) {
            return null;
        }
    }

    public boolean delete(String queue) {
        try {
            this.channel.queueDelete(queue);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean purge(String queue) {
        try {
            this.channel.queuePurge(queue);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Send to routing key with default exchange
     */
    public boolean send(String routingKey, byte[] body) {
        try {
            this.channel.basicPublish(StringHelper.EMPTY, routingKey, null, body);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Send to routing key with default exchange and priority
     */
    public boolean send(String routingKey, byte[] body, Integer priority) {
        try {
            BasicProperties.Builder basicProps = new BasicProperties.Builder();
            basicProps.priority(priority);

            this.channel.basicPublish(StringHelper.EMPTY, routingKey, basicProps.build(), body);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean start(String queueName, boolean ack, Consumer consumer) {
        try {
            String consumerTag = this.channel.basicConsume(queueName, ack, consumer);
            tags.put(queueName, consumerTag);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean stop(String queueName) {
        String consumerTag = tags.get(queueName);

        if (consumerTag != null) {
            try {
                this.channel.basicCancel(consumerTag);
                return true;
            } catch (IOException e) {
                return false;
            }
        }

        return false;
    }

    @Override
    public void close() throws Exception {
        channel.close();
        conn.close();
    }
}
