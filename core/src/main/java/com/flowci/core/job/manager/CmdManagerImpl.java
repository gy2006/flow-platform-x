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

import com.flowci.core.common.domain.Variables;
import com.flowci.core.job.domain.Job;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.service.PluginService;
import com.flowci.domain.CmdId;
import com.flowci.domain.CmdIn;
import com.flowci.domain.CmdType;
import com.flowci.core.plugin.domain.Variable;
import com.flowci.domain.StringVars;
import com.flowci.exception.ArgumentException;
import com.flowci.tree.Node;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author yang
 */
@Repository
public class CmdManagerImpl implements CmdManager {

    @Autowired
    private PluginService pluginService;

    @Override
    public CmdId createId(Job job, Node node) {
        return new CmdId(job.getId(), node.getPath().getPathInStr());
    }

    @Override
    public CmdIn createShellCmd(Job job, Node node) {
        // node envs has top priority;
        StringVars inputs = StringVars.merge(job.getContext(), node.getEnvironments());
        String script = node.getScript();
        boolean allowFailure = node.isAllowFailure();

        if (node.hasPlugin()) {
            Plugin plugin = pluginService.get(node.getPlugin());
            verifyPluginInput(inputs, plugin);

            script = plugin.getScript();
            allowFailure = plugin.isAllowFailure();
        }

        // create cmd based on plugin
        CmdIn cmd = new CmdIn(createId(job, node).toString(), CmdType.SHELL);
        cmd.setInputs(inputs);
        cmd.setAllowFailure(allowFailure);
        cmd.setEnvFilters(Sets.newHashSet(node.getExports()));
        cmd.setScripts(Lists.newArrayList(script));
        cmd.setPlugin(node.getPlugin());

        // get cmd work dir with default value flow id
        cmd.setWorkDir(inputs.get(Variables.Flow.WorkDir, job.getFlowId()));

        return cmd;
    }

    @Override
    public CmdIn createKillCmd() {
        return new CmdIn(UUID.randomUUID().toString(), CmdType.KILL);
    }

    private void verifyPluginInput(StringVars context, Plugin plugin) {
        for (Variable variable : plugin.getInputs()) {
            String value = context.get(variable.getName());

            if (!variable.verify(value)) {
                throw new ArgumentException(
                    "The illegal input {0} for plugin {1}", variable.getName(), plugin.getName());
            }
        }
    }
}
