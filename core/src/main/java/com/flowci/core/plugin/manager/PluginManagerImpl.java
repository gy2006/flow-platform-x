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

import com.flowci.core.config.ConfigProperties;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.PluginParser;
import com.flowci.core.plugin.domain.PluginRepo;
import com.flowci.core.plugin.event.RepoCloneEvent;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.CIException;
import com.flowci.exception.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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

    private static final String PluginFileName = "plugin.yml";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ConfigProperties appProperties;

    @Autowired
    private ThreadPoolTaskExecutor repoCloneExecutor;

    @Autowired
    private ApplicationContext context;

    private final Map<String, Plugin> pluginMap = new HashMap<>(10);

    @Override
    public Plugin get(String name) {
        Plugin plugin = pluginMap.get(name);
        if (Objects.isNull(plugin)) {
            throw new NotFoundException("The plugin {0} is not found", name);
        }
        return plugin;
    }

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

    @Override
    public void clone(List<PluginRepo> repos) {
        for (PluginRepo repo : repos) {
            repoCloneExecutor.execute(() -> {
                try {
                    Plugin plugin = clone(repo);
                    pluginMap.put(repo.getName(), plugin);
                    context.publishEvent(new RepoCloneEvent(this, plugin));
                    log.info("Plugin {} been clone", plugin);
                } catch (CIException e) {
                    log.warn(e.getMessage());
                } catch (GitAPIException | IOException e) {
                    log.warn("Unable to clone plugin repo {} {}", repo.getSource(), e.getMessage());
                }
            });
        }
    }

    private Plugin clone(PluginRepo repo) throws GitAPIException, IOException {
        File dir = getPluginRepoDir(repo.getName());
        GitProgressMonitor monitor = new GitProgressMonitor(repo.getSource(), dir);

        // pull from git when local repo exist
        if (Files.exists(dir.toPath())) {
            log.debug("The plugin repo existed: {}", repo);

            try (Git git = Git.open(dir)) {
                git.pull().setProgressMonitor(monitor).call();
            }

            return load(dir, repo);
        }

        // clone from git
        try (Git ignored = Git.cloneRepository()
            .setDirectory(dir)
            .setURI(repo.getSource())
            .setProgressMonitor(monitor)
            .call()) {
        }

        return load(dir, repo);
    }

    /**
     * Load plugin.yml from local repo
     * @throws IOException
     */
    private Plugin load(File dir, PluginRepo repo) throws IOException {
        Path pluginFile = Paths.get(dir.toString(), PluginFileName);
        if (!Files.exists(pluginFile)) {
            throw new NotFoundException("The 'plugin.yml' not found in plugin repo {0}", repo.getSource());
        }

        byte[] ymlInBytes = Files.readAllBytes(pluginFile);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(ymlInBytes)) {
            Plugin plugin = PluginParser.parse(inputStream);

            if (!repo.getName().equals(plugin.getName())) {
                throw new ArgumentException(
                    "Plugin name '{0}' not match the name defined in repo '{1}'", plugin.getName(), repo.getName());
            }

            if (!repo.getVersion().equals(plugin.getVersion())) {
                throw new ArgumentException(
                    "Plugin '{0}' version '{1}' not match the name defined in repo '{2}'",
                    plugin.getName(), plugin.getVersion().toString(), repo.getVersion().toString());
            }

            return plugin;
        }
    }

    private File getPluginRepoDir(String name) {
        String workspace = appProperties.getWorkspace();
        return Paths.get(workspace, "plugins", name).toFile();
    }

    private class GitProgressMonitor implements ProgressMonitor {

        private final String source;

        private final File target;

        public GitProgressMonitor(String source, File target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public void start(int totalTasks) {
            log.debug("Git - {} start: {}", source, totalTasks);
        }

        @Override
        public void beginTask(String title, int totalWork) {
            log.debug("Git - {} beginTask: {} {}", source, title, totalWork);
        }

        @Override
        public void update(int completed) {
            log.debug("Git - {} update: {}", source, completed);
        }

        @Override
        public void endTask() {
            log.debug("Git - {} endTask on {}", source, target);
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
