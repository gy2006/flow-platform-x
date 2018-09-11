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
import com.google.common.base.Strings;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.representer.Representer;

/**
 * @author yang
 */
public class YmlParser {

    private final static String DEFAULT_CHILD_NAME_PREFIX = "step-";

    private final static Constructor ROOT_YML_CONSTRUCTOR = new Constructor(RootNodeWrapper.class);

    private final static Representer ORDERED_SKIP_EMPTY_REPRESENTER = new OrderedSkipEmptyRepresenter();

    private final static DumperOptions DUMPER_OPTIONS = new DumperOptions();

    private final static LineBreak LINE_BREAK = LineBreak.getPlatformLineBreak();

    static {
        DUMPER_OPTIONS.setIndent(2);
        DUMPER_OPTIONS.setIndicatorIndent(0);
        DUMPER_OPTIONS.setExplicitStart(true);
        DUMPER_OPTIONS.setDefaultFlowStyle(FlowStyle.BLOCK);
        DUMPER_OPTIONS.setDefaultScalarStyle(ScalarStyle.PLAIN);
        DUMPER_OPTIONS.setLineBreak(LINE_BREAK);
    }

    /**
     * Create Node instance from yml
     */
    public static synchronized Node load(String defaultName, String yml) {
        Yaml yaml = new Yaml(ROOT_YML_CONSTRUCTOR);

        try {
            RootNodeWrapper root = yaml.load(yml);
            // set default flow name if not defined in yml
            if (Strings.isNullOrEmpty(root.name)) {
                root.name = defaultName;
            }

            // steps must be provided
            List<ChildNodeWrapper> steps = root.steps;
            if (Objects.isNull(steps) || steps.isEmpty()) {
                throw new YmlException("The 'step' must be defined");
            }

            return root.toNode(0);
        } catch (YAMLException e) {
            throw new YmlException(e.getMessage());
        }
    }

    public static synchronized String parse(Node root) {
        RootNodeWrapper rootWrapper = RootNodeWrapper.fromNode(root);

        Yaml yaml = new Yaml(ROOT_YML_CONSTRUCTOR, ORDERED_SKIP_EMPTY_REPRESENTER, DUMPER_OPTIONS);
        String dump = yaml.dump(rootWrapper);
        dump = dump.substring(dump.indexOf(LINE_BREAK.getString()) + 1);
        return dump;
    }

    @NoArgsConstructor
    private static class RootNodeWrapper {

        public static RootNodeWrapper fromNode(Node node) {
            RootNodeWrapper wrapper = new RootNodeWrapper();

            // set envs
            VariableMap environments = node.getEnvironments();
            for (Map.Entry<String, String> entry : environments.toStringMap().entrySet()) {
                wrapper.envs.put(entry.getKey(), entry.getValue());
            }

            // set children
            for (Node child : node.getChildren()) {
                wrapper.steps.add(ChildNodeWrapper.fromNode(child));
            }

            return wrapper;
        }

        public String name;

        public Selector selector = new Selector();

        public Map<String, String> envs = new LinkedHashMap<>();

        public List<ChildNodeWrapper> steps = new LinkedList<>();

        public Node toNode(int ignore) {
            Node node = new Node(name);
            node.setSelector(selector);
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
            for (ChildNodeWrapper child : steps) {
                node.getChildren().add(child.toNode(index++));
            }
        }
    }

    private static class ChildNodeWrapper extends RootNodeWrapper {

        public static ChildNodeWrapper fromNode(Node node) {
            ChildNodeWrapper wrapper = new ChildNodeWrapper();

            // set envs
            VariableMap environments = node.getEnvironments();
            for (Map.Entry<String, String> entry : environments.toStringMap().entrySet()) {
                wrapper.envs.put(entry.getKey(), entry.getValue());
            }

            wrapper.name = node.getName();
            wrapper.script = node.getScript();
            wrapper.plugin = node.getPlugin();
            wrapper.allowFailure = node.isAllowFailure() == Node.ALLOW_FAILURE_DEFAULT ? null : node.isAllowFailure();
            wrapper.isFinal = node.isFinal() == Node.IS_FINAL_DEFAULT ? null : node.isFinal();
            wrapper.condition = node.getCondition();

            for (Node child : node.getChildren()) {
                wrapper.steps.add(ChildNodeWrapper.fromNode(child));
            }

            return wrapper;
        }

        public String script;

        public String plugin;

        public Boolean allowFailure = false;

        public Boolean isFinal = false;

        public String condition;

        @Override
        public Node toNode(int index) {
            Node node = new Node(Strings.isNullOrEmpty(name) ? DEFAULT_CHILD_NAME_PREFIX + index : name);
            node.setScript(script);
            node.setPlugin(plugin);
            node.setAllowFailure(allowFailure);
            node.setCondition(condition);
            node.setFinal(isFinal);
            setEnvs(node);
            setChildren(node);
            return node;
        }
    }
}
