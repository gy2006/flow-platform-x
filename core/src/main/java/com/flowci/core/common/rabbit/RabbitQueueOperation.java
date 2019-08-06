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

package com.flowci.core.common.rabbit;


import com.rabbitmq.client.Connection;
import lombok.Getter;

import java.io.IOException;
import java.util.function.Function;

@Getter
public class RabbitQueueOperation extends RabbitOperation {

    private final String queueName;

    public RabbitQueueOperation(Connection conn, Integer concurrency, String queueName) throws IOException {
        super(conn, concurrency, queueName);
        this.queueName = queueName;
    }

    public String declare(boolean durable) {
        return super.declare(queueName, durable);
    }

    public String declare(boolean durable, Integer maxPriority) {
        return super.declare(queueName, durable, maxPriority);
    }

    public boolean delete() {
        removeConsumer();
        return super.delete(queueName);
    }

    public boolean purge() {
        return super.purge(queueName);
    }

    public boolean send(byte[] body) {
        return super.send(queueName, body);
    }

    public boolean send(byte[] body, Integer priority) {
        return super.send(queueName, body, priority);
    }

    public QueueConsumer getConsumer() {
        return super.getConsumer(queueName);
    }

    public QueueConsumer createConsumer(Function<Message, Boolean> consume) {
        return super.createConsumer(queueName, consume);
    }

    public boolean removeConsumer() {
        return super.removeConsumer(queueName);
    }
}
