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

package com.flowci.core.plugin;

import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.service.PluginService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collection;

import com.flowci.util.StringHelper;
import com.sun.org.apache.bcel.internal.generic.FSUB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * @author yang
 */
@RestController
@RequestMapping("/plugins")
public class PluginController {

    private static final String DefaultIconType = "image/svg+xml";

    @Autowired
    private PluginService pluginService;

    @GetMapping
    public Collection<Plugin> installed() {
        return pluginService.list();
    }

    @GetMapping(value = "/{name}/readme", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getReadMeContent(@PathVariable String name) {
        Plugin plugin = pluginService.get(name);
        byte[] raw = pluginService.getReadMe(plugin);
        return Base64.getEncoder().encodeToString(raw);
    }

    @GetMapping(value = "/{name}/icon", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getPluginIcon(@PathVariable String name) {
        Plugin plugin = pluginService.get(name);

        if (plugin.isHttpLinkIcon()) {
            return StringHelper.EMPTY;
        }

        byte[] raw = pluginService.getIcon(plugin);
        return Base64.getEncoder().encodeToString(raw);
    }

    @PostMapping("/reload")
    public void reload() {
        pluginService.reload();
    }
}
