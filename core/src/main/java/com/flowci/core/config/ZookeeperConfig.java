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

package com.flowci.core.config;

import com.flowci.core.helper.ThreadHelper;
import com.flowci.domain.ObjectWrapper;
import com.flowci.exception.CIException;
import com.flowci.zookeeper.LocalServer;
import com.flowci.zookeeper.ZookeeperClient;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class ZookeeperConfig {

    @Autowired
    private ConfigProperties config;

    @Bean("zkServerExecutor")
    public ThreadPoolTaskExecutor zookeeperThreadPool() {
        return ThreadHelper.createTaskExecutor(5, 1, 0, "zk-server-");
    }

    @Bean("zk")
    public ZookeeperClient zookeeperClient(ThreadPoolTaskExecutor zkServerExecutor) {
        if (config.getZookeeper().getEmbedded()) {
            startEmbeddedServer(zkServerExecutor);
            log.info("Embedded zookeeper been started ~");
        }

        String host = config.getZookeeper().getHost();
        Integer timeout = config.getZookeeper().getTimeout();
        Integer retry = config.getZookeeper().getRetry();

        ZookeeperClient client = new ZookeeperClient(host, retry, timeout);
        client.start();
        return client;
    }

    private void startEmbeddedServer(ThreadPoolTaskExecutor executor) {
        final Properties properties = new Properties();
        properties.setProperty("dataDir", Paths.get(config.getWorkspace(), "zookeeper").toString());
        properties.setProperty("clientPort", "2181");
        properties.setProperty("clientPortAddress", "0.0.0.0");
        properties.setProperty("tickTime", "1500");
        properties.setProperty("maxClientCnxns", "50");

        QuorumPeerConfig quorumPeerConfig = new QuorumPeerConfig();
        ServerConfig configuration = new ServerConfig();

        try {
            quorumPeerConfig.parseProperties(properties);
            configuration.readFrom(quorumPeerConfig);
        } catch (IOException | ConfigException e) {
            throw new CIException("Unable to start embedded zookeeper server: {}", e.getMessage());
        }

        CountDownLatch errorCounter = new CountDownLatch(1);
        ObjectWrapper<Exception> error = new ObjectWrapper<>();

        executor.execute(() -> {
            try {
                new LocalServer().runFromConfig(configuration);
            } catch (IOException e) {
                error.setValue(e);
                errorCounter.countDown();
            }
        });

        try {
            if (errorCounter.await(5, TimeUnit.SECONDS)) {
                throw new CIException("Unable to start embedded zookeeper: {}", error.getValue().getMessage());
            }
        } catch (InterruptedException e) {
            throw new CIException("Unable to start embedded zookeeper: {}", e.getMessage());
        }
    }
}
