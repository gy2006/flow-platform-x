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

package com.flowci.agent.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.domain.Jsonable;
import com.flowci.exception.JsonException;
import java.io.IOException;
import javax.persistence.AttributeConverter;

/**
 * JPA attribute converter
 *
 * @author yang
 */
public abstract class JsonConverter<T> implements AttributeConverter<T, String> {

    private final static ObjectMapper ObjectMapper = Jsonable.getMapper();

    public abstract TypeReference<T> getTypeReference();

    @Override
    public String convertToDatabaseColumn(T attribute) {
        try {
            return ObjectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new JsonException(e.getMessage());
        }
    }

    @Override
    public T convertToEntityAttribute(String dbData) {
        try {
            return ObjectMapper.readValue(dbData, getTypeReference());
        } catch (IOException e) {
            throw new JsonException(e.getMessage());
        }
    }
}
