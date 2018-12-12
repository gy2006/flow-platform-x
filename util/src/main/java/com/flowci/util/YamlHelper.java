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

package com.flowci.util;

import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.DumperOptions.LineBreak;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * @author yang
 */
public abstract class YamlHelper {

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

    public static Yaml create(Map<String, Integer> order, Class<? extends Object> theRoot) {
        Constructor rootConstructor = new Constructor(theRoot);
        Representer representer = new YamlOrderedSkipEmptyRepresenter(order);
        return new Yaml(rootConstructor, representer, DUMPER_OPTIONS);
    }

    public static Yaml create(Class<? extends Object> theRoot) {
        Constructor rootConstructor = new Constructor(theRoot);
        return new Yaml(rootConstructor);
    }
}
