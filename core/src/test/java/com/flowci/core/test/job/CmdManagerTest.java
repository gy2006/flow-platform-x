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

package com.flowci.core.test.job;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.YmlService;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.service.JobService;
import com.flowci.core.plugin.domain.Input;
import com.flowci.core.plugin.domain.ParentBody;
import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.ScriptBody;
import com.flowci.core.plugin.service.PluginService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.CmdIn;
import com.flowci.domain.StringVars;
import com.flowci.domain.VarType;
import com.flowci.domain.Vars;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import com.flowci.util.StringHelper;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.IOException;
import java.util.List;

public class CmdManagerTest extends SpringScenario {

    @Autowired
    private FlowService flowService;

    @Autowired
    private YmlService ymlService;

    @Autowired
    private JobService jobService;

    @MockBean
    private PluginService pluginService;

    @Autowired
    private CmdManager cmdManager;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_create_cmd_in_with_default_plugin_value() throws IOException {
        // init: setup mock plugin service
        Plugin plugin = createDummyPlugin();
        Mockito.when(pluginService.get(plugin.getName())).thenReturn(plugin);

        Plugin parent = createDummyParentPlugin();
        Mockito.when(pluginService.get(parent.getName())).thenReturn(parent);

        // given: flow and job
        Flow flow = flowService.create("hello");
        Yml yml = ymlService.saveYml(flow, StringHelper.toString(load("flow-with-plugin.yml")));

        Job job = jobService.create(flow, yml.getRaw(), Job.Trigger.MANUAL, new StringVars());
        Assert.assertNotNull(job);

        // when: create shell cmd
        Node root = YmlParser.load(flow.getName(), yml.getRaw());
        NodeTree tree = NodeTree.create(root);
        CmdIn cmdIn = cmdManager.createShellCmd(job, tree.get(NodePath.create(flow.getName(), "plugin-test")));
        Assert.assertNotNull(cmdIn);

        // then:
        Vars<String> inputs = cmdIn.getInputs();
        List<String> scripts = cmdIn.getScripts();
        Assert.assertEquals(2, scripts.size());

        Assert.assertEquals("gittest", cmdIn.getPlugin());
        Assert.assertEquals("test", inputs.get("GIT_STR_VAL"));
        Assert.assertEquals("60", inputs.get("GIT_DEFAULT_VAL"));

        // then: script should from self and parent, two inputs for parent
        Assert.assertEquals("echo \"plugin-test\"", scripts.get(0));
        Assert.assertEquals("echo ${P_VAR_1} ${P_VAR_2}", scripts.get(1));
        Assert.assertEquals("hello", inputs.get("P_VAR_1"));
        Assert.assertEquals("world", inputs.get("P_VAR_2"));
    }

    private Plugin createDummyPlugin() {
        Input intInput = new Input();
        intInput.setName("GIT_DEFAULT_VAL");
        intInput.setValue("60");
        intInput.setType(VarType.INT);
        intInput.setRequired(false);

        Input strInput = new Input();
        strInput.setName("GIT_STR_VAL");
        strInput.setValue("setup git str val");
        strInput.setType(VarType.STRING);
        strInput.setRequired(true);

        StringVars varsForParent = new StringVars();
        varsForParent.put("P_VAR_1", "hello");
        varsForParent.put("P_VAR_2", "world");

        ParentBody parent = new ParentBody();
        parent.setName("parent");
        parent.setEnvs(varsForParent);

        Plugin plugin = new Plugin();
        plugin.setName("gittest");
        plugin.setInputs(Lists.newArrayList(intInput, strInput));
        plugin.setBody(parent);

        return plugin;
    }

    private Plugin createDummyParentPlugin() {
        Input input1 = new Input();
        input1.setName("P_VAR_1");
        input1.setType(VarType.STRING);
        input1.setRequired(true);

        Input input2 = new Input();
        input2.setName("P_VAR_2");
        input2.setType(VarType.STRING);
        input2.setRequired(true);

        Plugin plugin = new Plugin();
        plugin.setName("parent");
        plugin.setInputs(Lists.newArrayList(input1, input2));
        plugin.setBody(new ScriptBody("echo ${P_VAR_1} ${P_VAR_2}"));

        return plugin;
    }
}
