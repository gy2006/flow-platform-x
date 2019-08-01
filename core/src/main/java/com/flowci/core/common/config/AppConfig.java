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
import com.flowci.core.common.adviser.AuthInterceptor;
import com.flowci.core.common.adviser.CrosInterceptor;
import com.flowci.core.common.domain.JsonablePage;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.user.User;
import com.flowci.domain.Jsonable;
import com.flowci.util.FileHelper;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
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
@EnableScheduling
public class AppConfig {

    private static final ObjectMapper Mapper = Jsonable.getMapper();

    private static final List<HttpMessageConverter<?>> DefaultConverters = ImmutableList.of(
        new ByteArrayHttpMessageConverter(),
        new MappingJackson2HttpMessageConverter(Mapper),
        new ResourceHttpMessageConverter(),
        new AllEncompassingFormHttpMessageConverter()
    );

    static {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Pageable.class, new JsonablePage.PageableDeserializer());
        Mapper.registerModule(module);
    }

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
    private void initLogDir() throws IOException {
        Path path = appProperties.getLogDir();
        FileHelper.createDirectory(path);
    }

    @PostConstruct
    public void initUploadDir() throws IOException {
        Path path = Paths.get(multipartProperties.getLocation());
        FileHelper.createDirectory(path);
    }

    @Bean("tmpDir")
    public Path tmpDir() {
        return Paths.get(appProperties.getWorkspace().toString(), "tmp");
    }

    @Bean("objectMapper")
    public ObjectMapper objectMapper() {
        return Mapper;
    }

    @Bean("restTemplate")
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(1000 * 15);
        factory.setConnectTimeout(1000 * 15);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setMessageConverters(DefaultConverters);
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
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new CrosInterceptor());
                registry.addInterceptor(authHandler())
                    .addPathPatterns("/flows/**")
                    .addPathPatterns("/jobs/**")
                    .addPathPatterns("/agents/**")
                    .addPathPatterns("/credentials/**")
                    .excludePathPatterns("/agents/connect")
                    .excludePathPatterns("/agents/logs/upload");
            }

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.clear();
                converters.addAll(DefaultConverters);
            }
        };
    }

    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();
        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor("s-event-"));
        return eventMulticaster;
    }
}
