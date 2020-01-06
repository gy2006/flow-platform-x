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

import com.flowci.exception.YmlException;
import com.flowci.tree.yml.FlowNode;
import com.flowci.tree.yml.StepNode;
import com.flowci.util.StringHelper;
import com.flowci.util.YamlHelper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import java.util.*;

import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * @author yang
 */
public class YmlParser {

    private final static LineBreak LINE_BREAK = LineBreak.getPlatformLineBreak();

    private final static Map<String, Integer> FieldsOrder = ImmutableMap.<String, Integer>builder()
        .put("name", 1)
        .put("envs", 2)
        .put("trigger", 3)
        .put("selector", 4)
        .put("allow_failure", 5)
        .put("tail", 6)
        .put("plugin", 7)
        .put("before", 8)
        .put("script", 9)
        .put("steps", 11)
        .build();

    /**
     * Create Node instance from yml
     */
    public static synchronized Node load(String defaultName, String yml) {
        Yaml yaml = YamlHelper.create(FlowNode.class);

        try {
            FlowNode root = yaml.load(yml);
            // set default flow name if not defined in yml
            if (Strings.isNullOrEmpty(root.getName())) {
                root.setName(defaultName);
            }

            if (!NodePath.validate(root.getName())) {
                throw new YmlException("Invalid name {0}", root.getName());
            }

            // steps must be provided
            List<StepNode> steps = root.getSteps();
            if (Objects.isNull(steps) || steps.isEmpty()) {
                throw new YmlException("The 'steps' must be defined");
            }

            Set<String> stepNames = new HashSet<>(steps.size());

            for (StepNode node : steps) {
                if (StringHelper.hasValue(node.getName()) && !NodePath.validate(node.getName())) {
                    throw new YmlException("Invalid name '{0}'", node.name);
                }

                if (!stepNames.add(node.name)) {
                    throw new YmlException("Duplicate step name {0}", node.name);
                }
            }

            return root.toNode(0);
        } catch (YAMLException e) {
            throw new YmlException(e.getMessage());
        }
    }

    public static synchronized String parse(Node root) {
        FlowNode flow = new FlowNode(root);
        Yaml yaml = YamlHelper.create(FieldsOrder, FlowNode.class);
        String dump = yaml.dump(flow);
        dump = dump.substring(dump.indexOf(LINE_BREAK.getString()) + 1);
        return dump;
    }
}
