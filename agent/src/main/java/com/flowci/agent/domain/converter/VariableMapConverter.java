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

import com.fasterxml.jackson.core.type.TypeReference;
import com.flowci.domain.VariableMap;

/**
 * VariableMap object convert for JPA
 *
 * @author yang
 */
public class VariableMapConverter extends JsonConverter<VariableMap> {

    private final static TypeReference<VariableMap> VariableMapType = new TypeReference<VariableMap>() {
    };

    @Override
    public TypeReference<VariableMap> getTypeReference() {
        return VariableMapType;
    }
}
