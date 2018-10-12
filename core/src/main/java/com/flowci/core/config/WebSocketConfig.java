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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * @author yang
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String jobsTopic = "/topic/jobs";

    /**
     * Ex: /topic/steps/{job id}
     */
    private final String stepsTopic = "/topic/steps";

    /**
     * Ex: /topic/logs/{cmd id}
     */
    private final String logsTopic = "/topic/logs";

    private final String agentsTopic = "/topic/agents";

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(jobsTopic, stepsTopic, logsTopic, agentsTopic);
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Bean("topicForJobs")
    public String topicForJobs() {
        return jobsTopic;
    }

    @Bean("topicForSteps")
    public String topicForSteps() {
        return stepsTopic;
    }

    @Bean("topicForLogs")
    public String topicForLogs() {
        return logsTopic;
    }

    @Bean("topicForAgents")
    public String topicForAgents() {
        return agentsTopic;
    }
}
