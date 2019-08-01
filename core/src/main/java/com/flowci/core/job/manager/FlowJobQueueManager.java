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

package com.flowci.core.job.manager;

import com.flowci.core.common.manager.RabbitQueueManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.NotFoundException;
import com.rabbitmq.client.Connection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FlowJobQueueManager implements AutoCloseable {

    @Autowired
    private Connection rabbitConnection;

    private final Map<String, RabbitQueueManager> queueManagerMap = new ConcurrentHashMap<>();

    public RabbitQueueManager create(String queueName) {
        try {
            RabbitQueueManager manager = new RabbitQueueManager(rabbitConnection, 1, queueName);
            queueManagerMap.put(queueName, manager);
            return manager;
        } catch (IOException e) {
            throw new NotAvailableException("Unable to create queue manager: {0}", e.getMessage());
        }
    }

    public RabbitQueueManager get(String queueName) {
        RabbitQueueManager manager = queueManagerMap.get(queueName);
        if (Objects.isNull(manager)) {
            throw new NotFoundException("RabbitQueueManager not found for flow job queue {0}", queueName);
        }
        return manager;
    }

    public void remove(String queueName) {
        RabbitQueueManager manager = queueManagerMap.remove(queueName);
        if (manager != null) {
            manager.delete();
        }
    }

    @Override
    public void close() {
        queueManagerMap.forEach((s, manager) -> manager.removeConsumer());
    }
}
