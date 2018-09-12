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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.flowci.core.plugin.domain.PluginRepo;
import com.flowci.core.plugin.manager.PluginManager;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.Version;
import com.flowci.util.StringHelper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class PluginManagerTest extends SpringScenario {

    @ClassRule
    public static WireMockRule wireMockRule = new WireMockRule(8000);

    @Autowired
    private PluginManager pluginManager;

    @Test
    public void should_load_plugin_repos_from_url() throws IOException {
        mockPluginRepo();

        List<PluginRepo> repos = pluginManager.load("http://localhost:8000/plugin/repo.json");
        Assert.assertEquals(1, repos.size());

        PluginRepo repo = repos.get(0);
        Assert.assertEquals("gitclone", repo.getName());
        Assert.assertEquals("https://github.com/yang-guo-2016/flowci-plugin-gitclone", repo.getSource());
        Assert.assertEquals("git clone plugin", repo.getDescription());
        Assert.assertEquals("gy@flow.ci", repo.getAuthor());
        Assert.assertEquals(Version.parse("0.0.1"), repo.getVersion());
    }

    private void mockPluginRepo() throws IOException {
        InputStream load = load("plugin-repo.json");
        stubFor(get(urlPathEqualTo("/plugin/repo.json"))
            .willReturn(aResponse()
                .withBody(StringHelper.toString(load))
                .withHeader("Content-Type", "application/json")));
    }
}
