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

package com.flowci.core.test.plugin;

import com.flowci.core.plugin.domain.*;
import com.flowci.domain.VarType;
import com.flowci.domain.Version;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class PluginParserTest {

    @Test
    public void should_parse_yml_to_plugin() {
        InputStream is = PluginParserTest.class.getClassLoader().getResourceAsStream("plugin.yml");
        Plugin plugin = PluginParser.parse(is);
        Assert.assertNotNull(plugin);

        Assert.assertEquals(1, plugin.getStatsTypes().size());
        Assert.assertEquals("gitclone", plugin.getName());
        Assert.assertEquals(Version.of(0, 0, 1, null), plugin.getVersion());
        Assert.assertEquals("src/icon.svg", plugin.getIcon());

        List<Input> inputs = plugin.getInputs();
        Assert.assertEquals(4, inputs.size());

        Input varForTimeout = inputs.get(3);
        Assert.assertNotNull(varForTimeout);
        Assert.assertEquals("GIT_TIMEOUT", varForTimeout.getName());
        Assert.assertEquals(VarType.INT, varForTimeout.getType());
        Assert.assertEquals(60, varForTimeout.getIntDefaultValue());

        Set<String> exports = plugin.getExports();
        Assert.assertEquals(2, exports.size());
        Assert.assertTrue(exports.contains("VAR_EXPORT_1"));
        Assert.assertTrue(exports.contains("VAR_EXPORT_2"));
    }

    @Test
    public void should_parse_yml_with_parent() {
        InputStream is = PluginParserTest.class.getClassLoader().getResourceAsStream("plugin_with_parent.yml");
        Plugin plugin = PluginParser.parse(is);
        Assert.assertNotNull(plugin);

        PluginBody body = plugin.getBody();
        Assert.assertNotNull(body);
        Assert.assertTrue(body instanceof ParentBody);

        ParentBody parent = (ParentBody) body;
        Assert.assertEquals("hello", parent.getName());
        Assert.assertEquals("0.1.0", parent.getVersion());
        Assert.assertEquals(1, parent.getEnvs().size());
        Assert.assertEquals("hello message", parent.getEnvs().get("GIT_URL_PARENT"));
    }

}
