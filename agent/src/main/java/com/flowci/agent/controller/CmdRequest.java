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

package com.flowci.agent.controller;

import com.flowci.domain.Cmd;
import com.flowci.domain.VariableMap;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author yang
 */
@Data
public class CmdRequest implements Serializable {

    private String id = UUID.randomUUID().toString();

    @NotNull
    private String scripts;

    private String workDir;

    private Long timeout = 1800L;

    private Map<String, String> inputs = Collections.emptyMap();

    private Set<String> filter = Collections.emptySet();

    public Cmd toCmd() {
        Cmd cmd = new Cmd();
        cmd.setId(id);
        cmd.setTimeout(timeout);
        cmd.setInputs(new VariableMap(inputs));
        cmd.setEnvFilters(filter);
        return cmd;
    }
}
