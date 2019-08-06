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

package com.flowci.core.common.coverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.domain.VariableMap;
import com.flowci.exception.ArgumentException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

/**
 * @author yang
 */
@RequiredArgsConstructor
public class VariableMapReader implements Converter<Document, VariableMap> {

    private final ObjectMapper objectMapper;

    @Override
    public VariableMap convert(Document source) {
        try {
            return objectMapper.readValue(source.toJson(), VariableMap.class);
        } catch (IOException e) {
            throw new ArgumentException("Cannot parse mongo doc {0} to VariableMap", source.toJson());
        }
    }
}
