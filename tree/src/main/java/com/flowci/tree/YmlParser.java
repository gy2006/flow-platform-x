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
import com.flowci.exception.YmlException;
import com.flowci.util.YamlHelper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NoArgsConstructor;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * @author yang
 */
public class YmlParser {

    private final static String DEFAULT_CHILD_NAME_PREFIX = "step-";

    private final static LineBreak LINE_BREAK = LineBreak.getPlatformLineBreak();

    private final static Map<String, Integer> FieldsOrder = ImmutableMap.<String, Integer>builder()
        .put("name", 1)
        .put("envs", 2)
        .put("condition", 3)
        .put("selector", 4)
        .put("allowFailure", 5)
        .put("isFinal", 6)
        .put("plugin", 7)
        .put("before", 8)
        .put("script", 9)
        .put("after", 10)
        .put("steps", 11)
        .build();

    /**
     * Create Node instance from yml
     */
    public static synchronized Node load(String defaultName, String yml) {
        Yaml yaml = YamlHelper.create(RootWrapper.class);

        try {
            RootWrapper root = yaml.load(yml);
            // set default flow name if not defined in yml
            if (Strings.isNullOrEmpty(root.name)) {
                root.name = defaultName;
            }

            // steps must be provided
            List<ChildWrapper> steps = root.steps;
            if (Objects.isNull(steps) || steps.isEmpty()) {
                throw new YmlException("The 'step' must be defined");
            }

            return root.toNode(0);
        } catch (YAMLException e) {
            throw new YmlException(e.getMessage());
        }
    }

    public static synchronized String parse(Node root) {
        RootWrapper rootWrapper = RootWrapper.fromNode(root);
        Yaml yaml = YamlHelper.create(FieldsOrder, RootWrapper.class);
        String dump = yaml.dump(rootWrapper);
        dump = dump.substring(dump.indexOf(LINE_BREAK.getString()) + 1);
        return dump;
    }

    @NoArgsConstructor
    private static class RootWrapper {

        public static RootWrapper fromNode(Node node) {
            RootWrapper wrapper = new RootWrapper();

            // set envs
            VariableMap environments = node.getEnvironments();
            for (Map.Entry<String, String> entry : environments.entrySet()) {
                wrapper.envs.put(entry.getKey(), entry.getValue());
            }

            // set children
            for (Node child : node.getChildren()) {
                wrapper.steps.add(ChildWrapper.fromNode(child));
            }

            return wrapper;
        }

        public String name;

        public Selector selector = new Selector();

        public Filter filter = new Filter();

        public Map<String, String> envs = new LinkedHashMap<>();

        public List<ChildWrapper> steps = new LinkedList<>();

        public Node toNode(int ignore) {
            Node node = new Node(name);
            node.setSelector(selector);
            node.setFilter(filter);
            setEnvs(node);
            setChildren(node);
            return node;
        }

        void setEnvs(Node node) {
            VariableMap environments = node.getEnvironments();
            for (Map.Entry<String, String> entry : envs.entrySet()) {
                environments.putString(entry.getKey(), entry.getValue());
            }
        }

        void setChildren(Node node) {
            int index = 1;
            for (ChildWrapper child : steps) {
                node.getChildren().add(child.toNode(index++));
            }
        }
    }

    private static class ChildWrapper extends RootWrapper {

        public static ChildWrapper fromNode(Node node) {
            ChildWrapper wrapper = new ChildWrapper();

            // set envs
            VariableMap environments = node.getEnvironments();
            for (Map.Entry<String, String> entry : environments.entrySet()) {
                wrapper.envs.put(entry.getKey(), entry.getValue());
            }

            wrapper.name = node.getName();
            wrapper.script = node.getScript();
            wrapper.plugin = node.getPlugin();
            wrapper.allowFailure = node.isAllowFailure() == Node.ALLOW_FAILURE_DEFAULT ? null : node.isAllowFailure();
            wrapper.isFinal = node.isFinal() == Node.IS_FINAL_DEFAULT ? null : node.isFinal();

            for (Node child : node.getChildren()) {
                wrapper.steps.add(ChildWrapper.fromNode(child));
            }

            return wrapper;
        }

        public String before;

        public String script;

        public String after;

        public String plugin;

        public Boolean allowFailure = false;

        public Boolean isFinal = false;

        @Override
        public Node toNode(int index) {
            Node node = new Node(Strings.isNullOrEmpty(name) ? DEFAULT_CHILD_NAME_PREFIX + index : name);
            node.setBefore(before);
            node.setScript(script);
            node.setAfter(after);
            node.setPlugin(plugin);
            node.setAllowFailure(allowFailure);
            node.setFinal(isFinal);
            setEnvs(node);
            setChildren(node);
            return node;
        }
    }
}
