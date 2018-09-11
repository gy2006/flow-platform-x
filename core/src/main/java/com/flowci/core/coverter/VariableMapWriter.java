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

package com.flowci.core.coverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.domain.VariableMap;
import com.flowci.exception.ArgumentException;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

/**
 * @author yang
 */
@RequiredArgsConstructor
public class VariableMapWriter implements Converter<VariableMap, Document> {

    private final ObjectMapper objectMapper;

    @Override
    public Document convert(VariableMap source) {
        try {
            String json = objectMapper.writeValueAsString(source);
            return Document.parse(json);
        } catch (JsonProcessingException e) {
            throw new ArgumentException("Cannot parse VariableMap to json");
        }
    }
}
