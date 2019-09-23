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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.flowci.util.StringHelper;

/**
 * @author yang
 */
public class StringVars extends LinkedHashMap<String, String> implements Vars<String> {

    public static final StringVars EMPTY = new StringVars(0);

    public static StringVars merge(Vars<String>... vars) {
        StringVars merged = new StringVars();

        for (Vars<String> item : vars) {
            if (item != null) {
                merged.merge(item);
            }
        }

        return merged;
    }

    public StringVars() {
        super();
    }

    public StringVars(int size) {
        super(size);
    }

    public StringVars(Map<String, String> data) {
        super(data.size() + 10);
        merge(data);
    }

    public StringVars(Vars<String> data) {
        super(data.size() + 10);
        merge(data);
    }

    @Override
    public Vars<String> putIfNotEmpty(String key, String value) {
        if (StringHelper.hasValue(value)) {
            put(key, value);
        }
        return this;
    }

    @Override
    public String get(String key, String defaultValue) {
        String value = get(key);
        return Objects.isNull(value) ? defaultValue : value;
    }

    @Override
    public void merge(Vars<String> other) {
        for (Map.Entry<String, String> entry : other.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    private void merge(Map<String, String> vars) {
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
