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

package com.flowci.core.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flowci.core.common.domain.JsonablePage;
import com.flowci.core.common.domain.Variables.App;
import com.flowci.domain.Jsonable;
import com.flowci.util.FileHelper;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author yang
 */
@Log4j2
@Configuration
@EnableScheduling
@EnableCaching
public class AppConfig {

    @Autowired
    private Environment env;

    @Autowired
    private ServerProperties serverProperties;

    @Autowired
    private MultipartProperties multipartProperties;

    @Autowired
    private ConfigProperties appProperties;

    @PostConstruct
    private void initWorkspace() throws IOException {
        Path path = appProperties.getWorkspace();
        FileHelper.createDirectory(path);
        FileHelper.createDirectory(tmpDir());
    }

    @PostConstruct
    private void initFlowDir() throws IOException {
        Path path = appProperties.getFlowDir();
        FileHelper.createDirectory(path);
    }

    @PostConstruct
    public void initUploadDir() throws IOException {
        Path path = Paths.get(multipartProperties.getLocation());
        FileHelper.createDirectory(path);
    }

    @Bean("serverAddress")
    public String serverAddress() throws URISyntaxException {
        String host = env.getProperty(App.Host, serverProperties.getAddress().toString());
        return new URIBuilder().setScheme("http")
                .setHost(host).setPort(serverProperties.getPort())
                .build()
                .toString();
    }

    @Bean("tmpDir")
    public Path tmpDir() {
        return Paths.get(appProperties.getWorkspace().toString(), "tmp");
    }

    @Bean("flowDir")
    public Path flowDir() {
        return appProperties.getFlowDir();
    }

    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = Jsonable.getMapper();

        SimpleModule module = new SimpleModule();
        module.addDeserializer(Pageable.class, new JsonablePage.PageableDeserializer());
        mapper.registerModule(module);

        return mapper;
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor("s-event-"));
        return eventMulticaster;
    }
}
