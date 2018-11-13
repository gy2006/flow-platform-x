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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.domain.VariableMap;
import com.google.common.base.Strings;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

/**
 * @author yang
 */
@Data
@EqualsAndHashCode(of = {"path"})
@ToString(of = {"path"})
public class Node implements Serializable {

    public final static boolean ALLOW_FAILURE_DEFAULT = false;

    public final static boolean IS_FINAL_DEFAULT = false;

    @NonNull
    private String name;

    @NonNull
    private NodePath path;

    @NonNull
    private VariableMap environments = new VariableMap();

    /**
     * Agent tags to set node running on which agent
     */
    private Selector selector;

    /**
     * Node start filter
     */
    private Filter filter = new Filter();

    /**
     * Node before groovy script;
     */
    private String before;

    /**
     * Node execute script, can be null
     */
    private String script;

    /**
     * Node after groovy script
     */
    private String after;

    /**
     * Plugin name
     */
    private String plugin;

    /**
     * Is allow failure
     */
    @NonNull
    private boolean allowFailure = ALLOW_FAILURE_DEFAULT;

    private boolean isFinal = IS_FINAL_DEFAULT;

    @NonNull
    private Integer order = 0;

    private Node parent;

    @NonNull
    private List<Node> children = new LinkedList<>();

    public Node(String name) {
        this.name = name;
        this.path = NodePath.create(name);
    }

    public String getPathAsString() {
        return path.getPathInStr();
    }

    public String getEnv(String name) {
        return environments.getString(name);
    }

    @JsonIgnore
    public boolean hasPlugin() {
        return !Strings.isNullOrEmpty(plugin);
    }

    @JsonIgnore
    public boolean hasBefore() {
        return !Strings.isNullOrEmpty(before);
    }
}
