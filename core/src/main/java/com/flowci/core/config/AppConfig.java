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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.flowci.core.adviser.AuthInterceptor;
import com.flowci.core.adviser.CrosInterceptor;
import com.flowci.core.domain.JsonablePage;
import com.flowci.core.helper.ThreadHelper;
import com.flowci.core.user.User;
import com.flowci.domain.Jsonable;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.data.domain.Pageable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author yang
 */
@Log4j2
@Configuration
@EnableCaching
@EnableScheduling
public class AppConfig {

    private static final ObjectMapper Mapper = Jsonable.getMapper();

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Pageable.class, new JsonablePage.PageableDeserializer());
        Mapper.registerModule(module);
    }

    @Autowired
    private ConfigProperties appProperties;

    @PostConstruct
    public void initWorkspace() {
        try {
            Path path = Paths.get(appProperties.getWorkspace());
            Files.createDirectory(path);
        } catch (FileAlreadyExistsException ignore) {

        } catch (IOException e) {
            log.error("Unable to init workspace directory: {}", appProperties.getWorkspace());
        }
    }

    @Bean
    public List<HttpMessageConverter<?>> defaultConverters() {
        return ImmutableList.of(
            new StringHttpMessageConverter(),
            new ByteArrayHttpMessageConverter(),
            new MappingJackson2HttpMessageConverter(Mapper),
            new ResourceHttpMessageConverter(),
            new AllEncompassingFormHttpMessageConverter()
        );
    }

    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        return Mapper;
    }

    @Bean("restTemplate")
    public RestTemplate restTemplate(List<HttpMessageConverter<?>> defaultConverters) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(1000 * 15);
        factory.setConnectTimeout(1000 * 15);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setMessageConverters(defaultConverters);
        return restTemplate;
    }

    @Bean("currentUser")
    public ThreadLocal<User> currentUser() {
        return new ThreadLocal<>();
    }

    @Bean
    public AuthInterceptor authHandler() {
        return new AuthInterceptor();
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(List<HttpMessageConverter<?>> defaultConverters) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new CrosInterceptor());
                registry.addInterceptor(authHandler())
                    .addPathPatterns("/flows/**")
                    .addPathPatterns("/jobs/**")
                    .addPathPatterns("/agents/**")
                    .addPathPatterns("/credentials/**")
                    .excludePathPatterns("/agents/connect");
            }

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.clear();
                converters.addAll(defaultConverters);
            }
        };
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        ThreadPoolTaskExecutor executor = ThreadHelper.createTaskExecutor(2, 2, 100, "spring-event-");
        executor.initialize();

        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(executor);
        return eventMulticaster;
    }
}
