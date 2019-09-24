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

package com.flowci.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * @author yang
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CmdIn extends CmdBase {

    @NonNull
    private CmdType type;

    @NonNull
    private List<String> scripts = new LinkedList<>();

    private String workDir;

    /**
     * Cmd timeout in seconds
     */
    @NonNull
    private Long timeout = 1800L;

    @NonNull
    private Vars<String> inputs = new StringVars();

    /**
     * Output env filters
     */
    @NonNull
    private Set<String> envFilters = Collections.emptySet();

    public CmdIn(String id, CmdType type) {
        setId(id);
        this.type = type;
    }

    @JsonIgnore
    public boolean hasWorkDir() {
        return !Strings.isNullOrEmpty(workDir);
    }
}
