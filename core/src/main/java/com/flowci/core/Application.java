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

package com.flowci.core;

import com.flowci.core.common.config.ConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author yang
 */
@SpringBootApplication
@Configuration
public class Application {

    @Bean("appProperties")
    @ConfigurationProperties(prefix = "app")
    public ConfigProperties appProperties() {
        return new ConfigProperties();
    }

    @Bean("adminProperties")
    @ConfigurationProperties(prefix = "app.admin")
    public ConfigProperties.Admin adminProperties() {
        return new ConfigProperties.Admin();
    }

    @Bean("zkProperties")
    @ConfigurationProperties(prefix = "app.zookeeper")
    public ConfigProperties.Zookeeper zkProperties() {
        return new ConfigProperties.Zookeeper();
    }

    @Bean("jobProperties")
    @ConfigurationProperties(prefix = "app.job")
    public ConfigProperties.Job jobProperties() {
        return new ConfigProperties.Job();
    }

    @Bean("pluginProperties")
    @ConfigurationProperties(prefix = "app.plugin")
    public ConfigProperties.Plugin pluginProperties() {
        return new ConfigProperties.Plugin();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
