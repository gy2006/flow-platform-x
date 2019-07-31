/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.common.manager;

import com.flowci.util.StringHelper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

/**
 * Create channel and handle the operation on the channel
 * enable to declare, delete, purge queue, start and stop consumer on queue
 *
 * @author yang
 */
@Log4j2
@Getter
public final class RabbitManager implements AutoCloseable {

    private final Connection conn;

    private final Channel channel;

    private final String name;

    private final Integer concurrency;

    private final ConcurrentHashMap<String, QueueConsumer> createdConsumers = new ConcurrentHashMap<>();

    public RabbitManager(Connection conn, Integer concurrency, String name) throws IOException {
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

    public String getConsumer(String queueName) {
        QueueConsumer queueConsumer = createdConsumers.get(queueName);
        if (Objects.isNull(queueConsumer)) {
            return null;
        }
        return queueConsumer.getConsumerTag();
    }

    public QueueConsumer createConsumer(String queue, Function<Message, Boolean> consume) {
        QueueConsumer queueConsumer = new QueueConsumer(queue, consume);
        createdConsumers.put(queue, queueConsumer);
        return queueConsumer;
    }

    public boolean removeConsumer(String queue) {
        QueueConsumer queueConsumer = createdConsumers.remove(queue);
        if (Objects.isNull(queueConsumer)) {
            return false;
        }

        return queueConsumer.stop();
    }

    @Override
    public void close() throws Exception {
        createdConsumers.forEach((s, queueConsumer) -> queueConsumer.stop());
        channel.close();
    }

    public class QueueConsumer extends DefaultConsumer {

        private final String queue;

        private final Function<Message, Boolean> consume;

        QueueConsumer(String queue, Function<Message, Boolean> consume) {
            super(channel);
            this.queue = queue;
            this.consume = consume;
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
            throws IOException {

            Boolean ingoreForNow = consume.apply(new Message(getChannel(), body, envelope));
        }

        public String start(boolean ack) {
            try {
                getChannel().basicConsume(queue, ack, this);
                log.info("[Consumer STARTED] queue {} with tag {}", queue, getConsumerTag());
                return getConsumerTag();
            } catch (IOException e) {
                log.warn(e.getMessage());
                return null;
            }
        }

        public boolean stop() {
            try {
                if (Objects.isNull(getConsumerTag())) {
                    return true; // not started
                }

                consume.apply(Message.STOP_SIGN);
                getChannel().basicCancel(getConsumerTag());
                log.info("[Consumer STOP] queue {} with tag {}", queue, getConsumerTag());
                return true;
            } catch (IOException e) {
                log.warn(e.getMessage());
                return false;
            }
        }
    }

    @Getter
    public static class Message {

        public static final Message STOP_SIGN = new Message(null, new byte[0], null);

        private final Channel channel;

        private final byte[] body;

        private final Envelope envelope;

        Message(Channel channel, byte[] body, Envelope envelope) {
            this.channel = channel;
            this.body = body;
            this.envelope = envelope;
        }

        public boolean sendAck() {
            try {
                getChannel().basicAck(envelope.getDeliveryTag(), false);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }
}
