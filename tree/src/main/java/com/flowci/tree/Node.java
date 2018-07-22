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

package com.flowci.tree;

import com.flowci.domain.VariableMap;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

/**
 * @author yang
 */
@EqualsAndHashCode(of = {"path"})
public class Node implements Serializable {

    public final static boolean ALLOW_FAILURE_DEFAULT = false;

    public final static boolean IS_FINAL_DEFAULT = false;

    @Setter
    @Getter
    @NonNull
    private String name;

    @Setter
    @Getter
    @NonNull
    private NodePath path;

    @Getter
    @Setter
    @NonNull
    private VariableMap environments = new VariableMap();

    /**
     * Node execute script, can be null
     */
    @Getter
    @Setter
    private String script;

    /**
     * Condition script
     */
    @Getter
    @Setter
    private String condition;

    @Getter
    @Setter
    private String plugin;

    /**
     * Is allow failure
     */
    @Getter
    @Setter
    @NonNull
    private boolean allowFailure = ALLOW_FAILURE_DEFAULT;

    @Getter
    @Setter
    private boolean isFinal = IS_FINAL_DEFAULT;

    @Getter
    @Setter
    @NonNull
    private Integer order = 0;

    @Getter
    @Setter
    private Node parent;

    @Getter
    @Setter
    @NonNull
    private List<Node> children = new LinkedList<>();

    public Node(String name) {
        this.name = name;
        this.path = NodePath.create(name);
    }

    public String getEnv(String name) {
        return environments.getString(name);
    }
}
