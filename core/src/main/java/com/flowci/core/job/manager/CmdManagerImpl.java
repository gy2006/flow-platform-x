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

package com.flowci.core.job.manager;

import com.flowci.core.domain.Variables;
import com.flowci.core.job.domain.CmdId;
import com.flowci.core.job.domain.Job;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.manager.PluginService;
import com.flowci.domain.Cmd;
import com.flowci.domain.CmdType;
import com.flowci.domain.Variable;
import com.flowci.domain.VariableMap;
import com.flowci.exception.ArgumentException;
import com.flowci.tree.Node;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.net.URI;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author yang
 */
@Repository
public class CmdManagerImpl implements CmdManager {

    private static final ParameterizedTypeReference<Page<String>> LoggingPageType =
        new ParameterizedTypeReference<Page<String>>() {
        };

    @Autowired
    private PluginService pluginService;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public CmdId createId(Job job, Node node) {
        return new CmdId(job.getId(), node.getPath().getPathInStr());
    }

    @Override
    public Cmd createShellCmd(Job job, Node node) {
        // node envs has top priority;
        VariableMap inputs = new VariableMap(job.getContext());
        inputs.merge(node.getEnvironments());

        String script = node.getScript();
        boolean allowFailure = node.isAllowFailure();

        if (node.hasPlugin()) {
            Plugin plugin = pluginService.get(node.getPlugin());
            verifyPluginInput(inputs, plugin);

            script = plugin.getScript();
            allowFailure = plugin.getAllowFailure();
        }

        // create cmd based on plugin
        Cmd cmd = new Cmd(createId(job, node).toString(), CmdType.SHELL);
        cmd.setInputs(inputs);
        cmd.setScripts(Lists.newArrayList(script));
        cmd.setWorkDir(inputs.get(Variables.AGENT_WORKSPACE));
        cmd.setAllowFailure(allowFailure);
        return cmd;
    }

    @Override
    public Cmd createKillCmd() {
        return new Cmd(UUID.randomUUID().toString(), CmdType.KILL);
    }

    @Override
    public Page<String> getLogs(String agentHost, String cmdId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(agentHost)
            .pathSegment("cmd", cmdId)
            .build().toUri();

        ResponseEntity<Page<String>> response =
            restTemplate.exchange(new RequestEntity<>(HttpMethod.GET, uri), LoggingPageType);

        return response.getBody();
    }

    private void verifyPluginInput(VariableMap context, Plugin plugin) {
        for (Variable variable : plugin.getInputs()) {
            String value = context.getString(variable.getName());
            if (Strings.isNullOrEmpty(value) && variable.isRequired()) {
                throw new ArgumentException(
                    "The input {0} is required for plugin {1}", variable.getName(), plugin.getName());
            }
        }
    }
}
