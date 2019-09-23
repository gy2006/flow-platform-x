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
import com.flowci.domain.VarValue;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import lombok.Getter;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;

@Getter
public class VariableMapConverter {

    private final ObjectMapper objectMapper;

    private final StringVarsReader strReader;

    private final StringVarsWriter strWriter;

    private final TypedVarsReader typedReader;

    private final TypedVarsWriter typedWriter;

    public VariableMapConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.strReader = new StringVarsReader();
        this.strWriter = new StringVarsWriter();
        this.typedReader = new TypedVarsReader();
        this.typedWriter = new TypedVarsWriter();
    }

    public class StringVarsReader implements Converter<Document, Vars<String>> {

        @Override
        public Vars<String> convert(Document source) {
            try {
                return objectMapper.readValue(source.toJson(), StringVars.class);
            } catch (IOException e) {
                throw new ArgumentException("Cannot parse mongo doc {0} to StringVars", source.toJson());
            }
        }
    }

    public class StringVarsWriter implements Converter<Vars<String>, Document> {

        @Override
        public Document convert(Vars<String> source) {
            try {
                String json = objectMapper.writeValueAsString(source);
                return Document.parse(json);
            } catch (JsonProcessingException e) {
                throw new ArgumentException("Cannot parse StringVars to json");
            }
        }
    }

    public class TypedVarsReader implements Converter<Document, Vars<VarValue>> {

        @Override
        public Vars<VarValue> convert(Document source) {
            try {
                return objectMapper.readValue(source.toJson(), TypedVars.class);
            } catch (IOException e) {
                throw new ArgumentException("Cannot parse mongo doc {0} to TypedVars", source.toJson());
            }
        }
    }

    public class TypedVarsWriter implements Converter<Vars<VarValue>, Document> {

        @Override
        public Document convert(Vars<VarValue> source) {
            try {
                String json = objectMapper.writeValueAsString(source);
                return Document.parse(json);
            } catch (JsonProcessingException e) {
                throw new ArgumentException("Cannot parse TypedVars to json");
            }
        }
    }
}
