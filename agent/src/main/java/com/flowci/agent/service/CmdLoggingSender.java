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

package com.flowci.agent.service;

import com.flowci.agent.executor.LoggingListener;
import com.flowci.domain.Cmd;
import com.flowci.domain.LogItem;
import com.flowci.util.StringHelper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Send real time log to logs exchange
 *
 * @author yang
 */
public class CmdLoggingSender implements LoggingListener {

    private static final MessageProperties MessageProperties = new MessageProperties();

    private final Cmd cmd;

    private final RabbitTemplate rabbitTemplate;

    private final String logsExchange;

    public CmdLoggingSender(Cmd cmd, RabbitTemplate rabbitTemplate, String logsExchange) {
        this.cmd = cmd;
        this.rabbitTemplate = rabbitTemplate;
        this.logsExchange = logsExchange;
    }

    @Override
    public void onLogging(LogItem item) {
        rabbitTemplate.send(logsExchange, StringHelper.EMPTY, new Message(item.toBytes(), MessageProperties));
    }
}
