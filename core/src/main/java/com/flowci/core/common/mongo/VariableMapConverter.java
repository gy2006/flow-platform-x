/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.common.mongo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.domain.StringVars;
import com.flowci.domain.TypedVars;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import java.io.IOException;
import java.util.Objects;
import lombok.Getter;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

@Getter
public class VariableMapConverter {

    private static final String typeSignField = "_VARS_TYPE_";

    private static final int codeForStringVars = 1;

    private static final int codeForTypedVars = 2;

    private final ObjectMapper objectMapper;

    private final Reader reader;

    private final Writer writer;

    public VariableMapConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.reader = new Reader();
        this.writer = new Writer();
    }

    public class Reader implements Converter<Document, Vars<?>> {

        @Override
        public Vars<?> convert(Document source) {
            try {
                Integer code = source.getInteger(typeSignField);
                source.remove(typeSignField);

                if (Objects.isNull(code) || code == codeForStringVars) {
                    return objectMapper.readValue(source.toJson(), StringVars.class);
                }

                if (code == codeForTypedVars) {
                    return objectMapper.readValue(source.toJson(), TypedVars.class);
                }

                throw new ArgumentException("Missing type code for vars");

            } catch (IOException e) {
                throw new ArgumentException("Cannot parse mongo doc {0} to StringVars", source.toJson());
            }
        }
    }

    public class Writer implements Converter<Vars<?>, Document> {

        @Override
        public Document convert(Vars<?> source) {
            try {
                String json = objectMapper.writeValueAsString(source);

                Document document = Document.parse(json);
                document.put(typeSignField, getTypeCode(source));

                return document;
            } catch (JsonProcessingException e) {
                throw new ArgumentException("Cannot parse StringVars to json");
            }
        }
    }

    private static int getTypeCode(Vars<?> vars) {
        if (vars instanceof StringVars) {
            return codeForStringVars;
        }

        if (vars instanceof TypedVars) {
            return codeForTypedVars;
        }

        return -1;
    }
}
