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

import com.flowci.util.StringHelper;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class QueueManager {

    @Autowired
    private RabbitTemplate queueTemplate;

    /**
     * Send to routing key with default exchange
     */
    public void send(String routingKey, Object content) {
        queueTemplate.convertAndSend(routingKey, content);
    }

    /**
     * Send to routing key with default exchange and priority
     */
    public void send(String routingKey, Object content, Integer priority) {
        queueTemplate.convertAndSend(StringHelper.EMPTY, routingKey, content, message -> {
            MessageProperties properties = message.getMessageProperties();
            properties.setPriority(priority);
            return message;
        });
    }
}
