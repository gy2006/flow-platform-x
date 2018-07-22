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

package com.flowci.core.flow.domain;

import com.flowci.core.domain.Mongoable;
import com.flowci.domain.VariableMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Document(collection = "flow")
@NoArgsConstructor
@ToString(of = {"name"}, callSuper = true)
public final class Flow extends Mongoable {

    @Getter
    @Setter
    @NonNull
    @Indexed(name = "index_flow_name")
    private String name;

    @Getter
    @Setter
    @NonNull
    private VariableMap variables = new VariableMap();

    public Flow(String name) {
        this.name = name;
    }
}
