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

package com.flowci.agent.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.flowci.agent.domain.converter.JsonConverter;
import com.flowci.agent.domain.converter.StringListConverter;
import com.flowci.agent.domain.converter.StringSetConverter;
import com.flowci.domain.Cmd;
import com.flowci.domain.VariableMap;
import java.util.List;
import java.util.Set;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author yang
 */
@Entity(name = "cmd")
public class AgentCmd extends Cmd {

    private final static TypeReference<VariableMap> VariableMapType = new TypeReference<VariableMap>() {
    };

    public static class VariableMapConverter extends JsonConverter<VariableMap> {

        @Override
        public TypeReference<VariableMap> getTypeReference() {
            return VariableMapType;
        }
    }

    @Id
    public String getId() {
        return super.getId();
    }

    @Override
    @Convert(converter = VariableMapConverter.class)
    public VariableMap getInputs() {
        return super.getInputs();
    }

    @Override
    @Convert(converter = StringListConverter.class)
    public List<String> getScripts() {
        return super.getScripts();
    }

    @Override
    @Convert(converter = StringSetConverter.class)
    public Set<String> getEnvFilters() {
        return super.getEnvFilters();
    }

    @Override
    public String getWorkDir() {
        return super.getWorkDir();
    }

    @Override
    public Long getTimeout() {
        return super.getTimeout();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
