/*
 * Copyright 2019 flow.ci
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

public abstract class Vars<V> extends LinkedHashMap<String, V> implements Serializable {

    Vars() {
        super();
    }

    Vars(int size) {
        super(size);
    }

    public Vars<V> putIfNotNull(String key, V value) {
        if (!Objects.isNull(value)) {
            put(key, value);
        }
        return this;
    }

    public V get(String key, V defaultValue) {
        V value = get(key);
        return Objects.isNull(value) ? defaultValue : value;
    }

    public Vars<V> merge(Vars<V> other) {
        for (Map.Entry<String, V> entry : other.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    void merge(Map<String, V> vars) {
        for (Map.Entry<String, V> entry : vars.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
}
