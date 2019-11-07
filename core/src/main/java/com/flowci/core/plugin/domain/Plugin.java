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

package com.flowci.core.plugin.domain;

import com.flowci.core.flow.domain.StatsType;
import com.flowci.domain.Version;
import java.util.LinkedList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@Document(collection = "plugins")
public class Plugin extends PluginRepoInfo {

    @Id
    private String id;

    private List<Variable> inputs = new LinkedList<>();

    // Plugin that supported statistic types
    private List<StatsType> statsTypes = new LinkedList<>();

    private boolean allowFailure = false;

    private String script;

    private String icon;

    public Plugin(String name, Version version) {
        this.setName(name);
        this.setVersion(version);
    }

    public void update(Plugin src) {
        this.setIcon(src.getIcon());
        this.setVersion(src.getVersion());
        this.setInputs(src.getInputs());
        this.setStatsTypes(src.getStatsTypes());
        this.setAllowFailure(src.isAllowFailure());
        this.setScript(src.getScript());
    }

    public boolean isHttpLinkIcon() {
        return icon != null && (icon.startsWith("http") || icon.startsWith("https"));
    }
}
