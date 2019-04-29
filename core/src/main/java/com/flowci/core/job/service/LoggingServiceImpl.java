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

package com.flowci.core.job.service;

import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.LogItem;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author yang
 */
@Log4j2
@Service
public class LoggingServiceImpl implements LoggingService {

    private static final Page<String> LogNotFound = new PageImpl<>(
            ImmutableList.of("Log not available"),
            PageRequest.of(0, 1),
            1L
    );

    @Autowired
    private String topicForLogs;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private ThreadPoolTaskExecutor logsExecutor;

    @Autowired
    private Path logDir;

    @Autowired
    private Cache<String, BufferedWriter> logWriterCache;

    @Autowired
    private Cache<String, BufferedReader> logReaderCache;

    @Override
    @RabbitListener(queues = "#{logsQueue.getName()}", containerFactory = "logsContainerFactory")
    public void processLogItem(Message message) {
        String logItemAsString = new String(message.getBody());
        log.debug(logItemAsString);

        // find cmd id from log item string
        int firstIndex = logItemAsString.indexOf(LogItem.SPLITTER);
        String cmdId = logItemAsString.substring(0, firstIndex);
        String destination = topicForLogs + "/" + cmdId;

        // send string message without cmd id
        String body = logItemAsString.substring(firstIndex + 1);
        simpMessagingTemplate.convertAndSend(destination, body);

        logsExecutor.execute(() -> writeLog(cmdId, body));
    }

    @Override
    public Page<String> readLogs(ExecutedCmd cmd, Pageable pageable) {
        BufferedReader reader = getReader(cmd.getId());

        if (Objects.isNull(reader)) {
            return LogNotFound;
        }

        try (Stream<String> lines = reader.lines()) {
            int i = pageable.getPageNumber() * pageable.getPageSize();

            List<String> logs = lines.skip(i)
                    .limit(pageable.getPageSize())
                    .collect(Collectors.toList());

            return new PageImpl<>(logs, pageable, cmd.getLogSize());
        }
    }

    private void writeLog(String cmdId, String body) {
        try {
            BufferedWriter writer = getWriter(cmdId);
            if (Objects.isNull(writer)) {
                return;
            }

            writer.write(body);
            writer.write(System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            log.debug(e);
        }
    }

    private BufferedWriter getWriter(String cmdId) {
        return logWriterCache.get(cmdId, key -> {
            log.debug("New buffer writer for cmd id: {}", key);

            try {
                Path target = Paths.get(logDir.toString(), key + ".log");
                return Files.newBufferedWriter(target);
            } catch (IOException e) {
                return null;
            }
        });
    }

    private BufferedReader getReader(String cmdId) {
        return logReaderCache.get(cmdId, key -> {
            try {
                Path target = Paths.get(logDir.toString(), key + ".log");
                return Files.newBufferedReader(target);
            } catch (IOException e) {
                return null;
            }
        });
    }
}
