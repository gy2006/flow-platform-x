/*
 * Copyright 2019 flow.ci
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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.AgentInterceptor;
import com.flowci.core.auth.AuthInterceptor;
import com.flowci.core.common.adviser.CrosInterceptor;
import com.flowci.core.common.helper.JacksonHelper;
import com.flowci.core.user.domain.User;
import com.flowci.domain.Vars;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public ThreadLocal<User> currentUser() {
        return new ThreadLocal<>();
    }

    @Bean
    public AuthInterceptor authHandler() {
        return new AuthInterceptor();
    }

    @Bean
    public AgentInterceptor agentInterceptor() {
        return new AgentInterceptor();
    }

    @Bean
    public Class<?> httpJacksonMixin() {
        return VarsMixin.class;
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new CrosInterceptor());

                registry.addInterceptor(authHandler())
                    .addPathPatterns("/users/**")
                    .addPathPatterns("/flows/**")
                    .addPathPatterns("/jobs/**")
                    .addPathPatterns("/agents/**")
                    .addPathPatterns("/credentials/**")
                    .addPathPatterns("/auth/logout")
                    .excludePathPatterns("/agents/connect")
                    .excludePathPatterns("/agents/resource")
                    .excludePathPatterns("/agents/logs/upload");

                registry.addInterceptor(agentInterceptor())
                        .addPathPatterns("/agents/connect")
                        .addPathPatterns("/agents/resource")
                        .addPathPatterns("/agents/logs/upload");
            }

            @Override
            public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
                converters.clear();

                ObjectMapper mapperForHttp = JacksonHelper.create();
                mapperForHttp.addMixIn(Vars.class, httpJacksonMixin());

                final List<HttpMessageConverter<?>> DefaultConverters = ImmutableList.of(
                    new ByteArrayHttpMessageConverter(),
                    new MappingJackson2HttpMessageConverter(mapperForHttp),
                    new ResourceHttpMessageConverter(),
                    new AllEncompassingFormHttpMessageConverter()
                );

                converters.addAll(DefaultConverters);
            }
        };
    }

    /**
     * Jackson mixin to ignore meta type for Vars
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    private interface VarsMixin {

    }
}
