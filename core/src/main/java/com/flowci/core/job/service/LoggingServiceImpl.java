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

import com.flowci.core.common.helper.CacheHelper;
import com.flowci.core.common.rabbit.RabbitChannelOperation;
import com.flowci.core.common.rabbit.RabbitOperation;
import com.flowci.core.common.rabbit.RabbitQueueOperation;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.LogItem;
import com.flowci.exception.NotFoundException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.google.common.collect.ImmutableList;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    private static final int FileBufferSize = 8000; // ~8k

    private Cache<String, BufferedReader> logReaderCache =
        CacheHelper.createLocalCache(10, 60, new ReaderCleanUp());

    @Autowired
    private String topicForLogs;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private Path logDir;

    @Autowired
    private RabbitQueueOperation loggingQueueManager;

    @EventListener(ContextRefreshedEvent.class)
    public void onStart() {
        RabbitChannelOperation.QueueConsumer consumer = loggingQueueManager.createConsumer(message -> {
            if (message == RabbitOperation.Message.STOP_SIGN) {
                return true;
            }

            final String msg = new String(message.getBody(), StandardCharsets.UTF_8);
            handleLoggingItem(msg);
            return true;
        });

        consumer.start(true);
    }

    @Override
    public void handleLoggingItem(String message) {
        log.debug("[LOG]: {}", message);

        // find cmd id from log item string
        int firstIndex = message.indexOf(LogItem.SPLITTER);
        String cmdId = message.substring(0, firstIndex);
        String destination = topicForLogs + "/" + cmdId;

        // send string message without cmd id
        String body = message.substring(firstIndex + 1);
        simpMessagingTemplate.convertAndSend(destination, body);
    }

    @Override
    public Page<String> read(ExecutedCmd cmd, Pageable pageable) {
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
        } finally {
            try {
                reader.reset();
            } catch (IOException e) {
                // reset will be failed if all lines been read
                logReaderCache.invalidate(cmd.getId());
            }
        }
    }

    @Override
    public Path save(String fileName, InputStream stream) {
        try {
            Path target = Paths.get(logDir.toString(), fileName);
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            log.debug(e);
            return null;
        }
    }

    @Override
    public Resource get(String cmdId, boolean isRaw) {
        Path logPath = isRaw ? getRawPath(cmdId) : getLogPath(cmdId);
        try {
            Resource resource = new UrlResource(logPath.toUri());
            if (resource.exists()) {
                return resource;
            }

            throw new NotFoundException("Log not available");
        } catch (MalformedURLException e) {
            throw new NotFoundException("Log not available");
        }
    }

    private BufferedReader getReader(String cmdId) {
        return logReaderCache.get(cmdId, key -> {
            try {
                Path target = getRawPath(cmdId);
                BufferedReader reader = new BufferedReader(new FileReader(target.toFile()), FileBufferSize);
                reader.mark(1);
                return reader;
            } catch (IOException e) {
                return null;
            }
        });
    }

    private Path getRawPath(String cmdId) {
        return Paths.get(logDir.toString(), cmdId + ".raw.log");
    }

    private Path getLogPath(String cmdId) {
        return Paths.get(logDir.toString(), cmdId + ".log");
    }

    private class ReaderCleanUp implements RemovalListener<String, BufferedReader> {

        @Override
        public void onRemoval(String key, BufferedReader reader, RemovalCause cause) {
            if (Objects.isNull(reader)) {
                return;
            }

            try {
                reader.close();
            } catch (IOException e) {
                log.debug(e);
            }
        }
    }
}
