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

import com.flowci.domain.Variable;
import com.flowci.domain.Version;
import java.io.Serializable;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@Getter
@Setter
@EqualsAndHashCode(of = {"name"})
@ToString(of = {"name", "version"})
public class Plugin implements Serializable {

    private String name;

    private Version version;

    private List<Variable> inputs;

    private String script;

    public Plugin(String name, Version version) {
        this.name = name;
        this.version = version;
    }
}
