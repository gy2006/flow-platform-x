/*
 * Copyright 2018 fir.im
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

import com.flowci.domain.Variable.ValueType;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author yang
 */
public class VariableMap implements Serializable {

    private final static Map<ValueType, ValueTypeValidator> ValueValidators = new HashMap<>(1);

    static {
        ValueValidators.put(ValueType.LIST, new ListValidator());
    }

    private final Map<Variable, Object> values = new HashMap<>(10);

    public void addString(String key, String value) {
        Variable var = new Variable(key);
        values.put(var, value);
    }

    public void addInt(String key, Integer value) {
        Variable var = new Variable(key, ValueType.INT);
        values.put(var, value);
    }

    public String getString(String key) {
        return values.get(new Variable(key)).toString();
    }

    public Integer getInteger(String key) {
        return (Integer) values.get(new Variable(key));
    }

    private interface ValueTypeValidator {

        /**
         * Validate value is match the type
         *
         * @throws IllegalArgumentException if not matched
         */
        void validate(Variable variable, Object value);
    }

    private static class ListValidator implements ValueTypeValidator {

        @Override
        public void validate(Variable variable, Object value) {
            List<String> values = variable.getValues();

            if (Objects.isNull(values)) {
                return;
            }

            if (!(value instanceof String)) {
                throw new IllegalArgumentException("The list value item should be String");
            }

            if (!values.contains(value)) {
                throw new IllegalArgumentException("The value should be one of " + String.join(",", values));
            }
        }
    }
}
