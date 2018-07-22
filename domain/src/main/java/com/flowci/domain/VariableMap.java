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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.flowci.domain.Variable.ValueType;
import com.flowci.domain.VariableMap.VariableKeyDeserializer;
import com.flowci.domain.VariableMap.VariableKeySerializer;
import com.flowci.util.StringHelper;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author yang
 */
@JsonSerialize(keyUsing = VariableKeySerializer.class)
@JsonDeserialize(keyUsing = VariableKeyDeserializer.class)
public class VariableMap extends LinkedHashMap<Variable, String> implements Serializable {

    /**
     * Convert Variable object to base64 string
     */
    public static class VariableKeySerializer extends JsonSerializer<Variable> {

        @Override
        public void serialize(Variable value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            String json = Jsonable.getMapper().writeValueAsString(value);
            String variableKey = StringHelper.toBase64(json);
            gen.writeFieldName(variableKey);
        }
    }

    /**
     * Convert base64 string to Variable
     */
    public static class VariableKeyDeserializer extends KeyDeserializer {

        @Override
        public Object deserializeKey(String base64, DeserializationContext ctxt) throws IOException {
            String json = StringHelper.fromBase64(base64);
            return Jsonable.getMapper().readValue(json, Variable.class);
        }
    }

    public VariableMap() {
        super();
    }

    public void putString(String key, String value) {
        Variable var = new Variable(key);
        put(var, value);
    }

    public void putInt(String key, Integer value) {
        Variable var = new Variable(key, ValueType.INT);
        put(var, value.toString());
    }

    public String getString(String key) {
        return get(new Variable(key));
    }

    public Integer getInteger(String key) {
        return Integer.parseInt(get(new Variable(key)));
    }

    public Map<String, String> toStringMap() {
        if (isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>(size());
        for (Map.Entry<Variable, String> entry : entrySet()) {
            map.put(entry.getKey().getName(), entry.getValue());
        }
        return map;
    }

    public void reset(Map<String, String> vars) {
        clear();
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            putString(entry.getKey(), entry.getValue());
        }
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
