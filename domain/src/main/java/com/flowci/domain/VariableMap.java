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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author yang
 */
public class VariableMap extends LinkedHashMap<String, String> implements Serializable {

    public static final VariableMap EMPTY = new VariableMap(0);

    public static VariableMap merge(VariableMap... variables) {
        VariableMap merged = new VariableMap();

        for (VariableMap item : variables) {
            merged.putAll(item);
        }

        return merged;
    }

    public VariableMap() {
        super();
    }

    public VariableMap(int size) {
        super(size);
    }

    public VariableMap(Map<String, String> data) {
        super(data.size() + 10);
        load(data);
    }

    public VariableMap merge(VariableMap other) {
        for (Map.Entry<String, String> entry : other.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return Objects.isNull(value) ? defaultValue : value;
    }

    public void reset(Map<String, String> vars) {
        clear();
        load(vars);
    }

    public void load(Map<String, String> vars) {
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
