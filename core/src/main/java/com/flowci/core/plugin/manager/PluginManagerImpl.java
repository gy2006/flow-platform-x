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

package com.flowci.core.plugin.manager;

import com.flowci.core.plugin.domain.PluginRepo;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * @author yang
 */
@Log4j2
@Component
public class PluginManagerImpl implements PluginManager {

    private static final ParameterizedTypeReference<List<PluginRepo>> RepoListType =
        new ParameterizedTypeReference<List<PluginRepo>>() {
        };

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public List<PluginRepo> load(String repoUrl) {
        try {
            ResponseEntity<List<PluginRepo>> response = restTemplate
                .exchange(new RequestEntity<>(HttpMethod.GET, URI.create(repoUrl)), RepoListType);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

            log.warn("Unable to load plugin repo '{}' since http status code {}", repoUrl, response.getStatusCode());
            return Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("Unable to load plugin repo '{}' : {}", repoUrl, e.getMessage());
            return Collections.emptyList();
        }
    }
}
