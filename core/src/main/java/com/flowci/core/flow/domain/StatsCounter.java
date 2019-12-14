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

package com.flowci.core.flow.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yang
 */
public class StatsCounter extends HashMap<String, Float> {

    public static StatsCounter from(Map<String, Float> data) {
        StatsCounter counter = new StatsCounter();
        counter.putAll(data);
        return counter;
    }

    public void add(StatsCounter another) {
        for (Map.Entry<String, Float> entry : another.entrySet()) {
            String key = entry.getKey();
            Float localValue = this.getOrDefault(key, 0.0F);
            this.put(key, localValue + entry.getValue());
        }
    }
}
