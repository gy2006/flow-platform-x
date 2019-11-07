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

package com.flowci.core.plugin.service;

import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.PluginRepoInfo;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * @author yang
 */
public interface PluginService {

    /**
     * List all installed plugin
     */
    Collection<Plugin> list();

    /**
     * Get plugin by name
     */
    Plugin get(String name);

    /**
     * Get ReadMe.md content
     */
    byte[] getReadMe(Plugin plugin);

    /**
     * Get icon byte array
     */
    byte[] getIcon(Plugin plugin);

    /**
     * Get repo file path
     */
    Path getDir(Plugin plugin);

    /**
     * Load plugin repo info
     */
    List<PluginRepoInfo> load(String repoUrl);

    /**
     * Git clone plugin repos in Async
     */
    void clone(List<PluginRepoInfo> repos);

    /**
     * Reload default plugin repo
     */
    void reload();
}
