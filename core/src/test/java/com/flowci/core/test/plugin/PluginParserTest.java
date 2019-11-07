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

import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.PluginParser;
import com.flowci.domain.Version;
import java.io.InputStream;
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
        Assert.assertEquals(3, plugin.getInputs().size());
        Assert.assertEquals(Boolean.TRUE, plugin.isAllowFailure());

        Assert.assertNotNull(plugin.getTags());
        Assert.assertEquals(2, plugin.getTags().size());
        Assert.assertTrue(plugin.getTags().contains("git"));
        Assert.assertTrue(plugin.getTags().contains("clone"));
    }

}
