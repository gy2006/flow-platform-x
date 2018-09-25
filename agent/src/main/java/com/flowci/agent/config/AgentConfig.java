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

package com.flowci.agent.config;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.domain.Jsonable;
import com.flowci.domain.Settings;
import com.flowci.domain.http.ResponseMessage;
import com.flowci.exception.StatusException;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author yang
 */
@Log4j2
@Configuration
@Order(1)
public class AgentConfig implements WebMvcConfigurer {

    private static final List<HttpMessageConverter<?>> DefaultConverters = Lists.newArrayList(
        new ByteArrayHttpMessageConverter(),
        new MappingJackson2HttpMessageConverter(Jsonable.getMapper()),
        new ResourceHttpMessageConverter(),
        new AllEncompassingFormHttpMessageConverter()
    );

    private static final SimpleClientHttpRequestFactory HttpClientFactory = new SimpleClientHttpRequestFactory();

    private static final RestTemplate RestTemplate = new RestTemplate(HttpClientFactory);

    static {
        HttpClientFactory.setReadTimeout(1000 * 15);
        HttpClientFactory.setConnectTimeout(1000 * 15);
        RestTemplate.setMessageConverters(DefaultConverters);
    }

    @Autowired
    private AgentProperties agentProperties;

    @Bean("workspace")
    public Path workspace() {
        Path path = Paths.get(agentProperties.getWorkspace());
        return initDir(path, "Unable to init workspace");
    }

    @Bean("loggingDir")
    public Path loggingDir() {
        Path path = Paths.get(agentProperties.getLoggingDir());
        return initDir(path, "Unable to init logging dir");
    }

    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        return Jsonable.getMapper();
    }

    @Bean("restTemplate")
    public RestTemplate restTemplate() {
        return RestTemplate;
    }

    @Bean("agentSettings")
    public Settings getConfigFromServer() {
        URI uri = UriComponentsBuilder.fromHttpUrl(agentProperties.getServerUrl())
            .pathSegment("agents", "connect")
            .queryParam("token", agentProperties.getToken())
            .build()
            .toUri();

        ParameterizedTypeReference<ResponseMessage<Settings>> type =
            new ParameterizedTypeReference<ResponseMessage<Settings>>() {
            };

        try {
            RequestEntity<Object> requestEntity = new RequestEntity<>(HttpMethod.GET, uri);
            ResponseMessage<Settings> message = RestTemplate.exchange(requestEntity, type).getBody();

            if (message.getCode() != 200) {
                throw new StatusException("Service not available: {0}", message.getMessage());
            }

            log.info("Settings been loaded: {}", message.getData());
            return message.getData();
        } catch (Throwable e) {
            throw new RuntimeException("Unable to load settings: " + e.getMessage());
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods(GET.name(), POST.name(), PUT.name(), DELETE.name())
            .allowedHeaders("*");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.clear();
        converters.addAll(DefaultConverters);
    }

    private Path initDir(Path path, String errMsg) {
        try {
            return Files.createDirectories(path);
        } catch (FileAlreadyExistsException ignore) {
            return path;
        } catch (IOException e) {
            log.error("{}: {}", errMsg, path);
            throw new RuntimeException(e.getMessage());
        }
    }
}
