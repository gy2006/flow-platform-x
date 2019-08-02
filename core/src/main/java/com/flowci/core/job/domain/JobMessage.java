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

package com.flowci.core.job.domain;

import com.flowci.core.common.manager.RabbitManager;
import com.rabbitmq.client.Envelope;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Present incoming pending job message from queue,
 *
 * @author yang
 */
@Data
@EqualsAndHashCode(of = "queueName")
@Document(collection = "job_message")
public class JobMessage {

    @Id
    private String queueName;

    private String exchange;

    private byte[] body;

    private boolean redeliver;

    private Long deliveryTag;

    public JobMessage() {
    }

    public JobMessage(RabbitManager.Message message) {
        Envelope envelope = message.getEnvelope();
        this.body = message.getBody();
        this.queueName = envelope.getRoutingKey();
        this.exchange = envelope.getExchange();
        this.deliveryTag = envelope.getDeliveryTag();
        this.redeliver = envelope.isRedeliver();
    }

    public Envelope toEnvelop() {
        return new Envelope(this.deliveryTag, this.redeliver, this.exchange, this.queueName);
    }
}
