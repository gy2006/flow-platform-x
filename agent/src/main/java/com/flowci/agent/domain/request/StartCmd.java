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

package com.flowci.agent.domain.request;

import com.flowci.domain.Cmd;
import com.flowci.domain.CmdType;
import com.flowci.domain.VariableMap;
import com.flowci.util.StringHelper;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

/**
 * @author yang
 */
@Data
public class StartCmd {

    private String id = UUID.randomUUID().toString();

    private CmdType type = CmdType.SHELL;

    private String scripts = StringHelper.EMPTY;

    /**
     * Support env variable like ${HOME}
     */
    private String workDir;

    private Long timeout = 1800L;

    private Map<String, String> inputs = Collections.emptyMap();

    private Set<String> filter = Collections.emptySet();

    public Cmd toCmd() {
        Cmd cmd = new Cmd(id, type);
        cmd.setScripts(Lists.newArrayList(scripts));
        cmd.setWorkDir(workDir);
        cmd.setTimeout(timeout);
        cmd.setInputs(new VariableMap(inputs));
        cmd.setEnvFilters(filter);
        return cmd;
    }
}
