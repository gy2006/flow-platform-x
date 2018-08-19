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

import com.flowci.zookeeper.LocalServer;
import com.flowci.zookeeper.ZookeeperClient;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yang
 */
@Log4j2
@Configuration
public class ZookeeperConfig {

    @Autowired
    private ConfigProperties config;

    @PostConstruct
    private void startLocalServer() {
        if (!config.getZookeeper().getEmbedded()) {
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("dataDir", Paths.get(config.getWorkspace(), "zookeeper").toString());
        properties.setProperty("clientPort", "2181");
        properties.setProperty("clientPortAddress", "0.0.0.0");
        properties.setProperty("tickTime", "1500");
        properties.setProperty("maxClientCnxns", "50");

        try {
            QuorumPeerConfig quorumPeerConfig = new QuorumPeerConfig();
            quorumPeerConfig.parseProperties(properties);

            ServerConfig configuration = new ServerConfig();
            configuration.readFrom(quorumPeerConfig);

            new LocalServer().runFromConfig(configuration);
        } catch (IOException e) {
            log.error("Unable to start embedded zookeeper server: {}", e.getMessage());
        } catch (ConfigException e) {
            log.error("Unable to init embedded zookeeper server config: {}", e.getMessage());
        }
    }

    @Bean("zk")
    public ZookeeperClient zookeeperClient() {
        String host = config.getZookeeper().getHost();
        Integer timeout = config.getZookeeper().getTimeout();
        Integer retry = config.getZookeeper().getRetry();

        ZookeeperClient client = new ZookeeperClient(host, retry, timeout);
        client.start();
        return client;
    }

}
