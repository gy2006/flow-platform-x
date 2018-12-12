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

package com.flowci.agent.manager;

import com.flowci.agent.config.AgentProperties;
import com.flowci.exception.NotAvailableException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author yang
 */
@Log4j2
@Service
public class PluginManagerImpl implements PluginManager {

    @Autowired
    private AgentProperties agentProperties;

    @Autowired
    private Path pluginDir;

    @Override
    public Path getPath() {
        return pluginDir;
    }

    @Override
    public void load(String name) {
        URI uri = UriComponentsBuilder.fromUri(URI.create(agentProperties.getServerUrl()))
            .pathSegment("/git/plugins", name)
            .build()
            .toUri();

        try {
            log.info("Start to load plugin: {}", name);
            Path dir = Paths.get(pluginDir.toString(), name);
            GitProgressMonitor monitor = new GitProgressMonitor(uri.toString(), dir.toFile());

            // pull from git when local repo exist
            if (Files.exists(dir)) {
                log.debug("The plugin existed: {}", name);

                try (Git git = Git.open(dir.toFile())) {
                    git.pull().setProgressMonitor(monitor).call();
                }

                return;
            }

            // clone from git
            try (Git ignored = Git.cloneRepository()
                .setDirectory(dir.toFile())
                .setURI(uri.toString())
                .setProgressMonitor(monitor)
                .call()) {
            }
        } catch (IOException | GitAPIException e) {
            log.warn(e.getMessage());
            throw new NotAvailableException("Cannot get plugin {0}", name);
        }
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
