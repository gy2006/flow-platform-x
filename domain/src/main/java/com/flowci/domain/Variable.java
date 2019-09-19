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

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@Getter
@Setter
@ToString(of = {"name"})
@EqualsAndHashCode(of = {"name"})
@NoArgsConstructor
public class Variable implements Serializable {

    public enum ValueType {

        STRING,

        INTEGER,

        HTTP_URL,

        SSH_URL,

        EMAIL,

        ENCRYPTED
    }

    private String name;

    private String alias;

    private List<ValueType> types = Lists.newArrayList(ValueType.STRING);

    private boolean required = true;

    public Variable(String name) {
        this.name = name;
    }

    public Variable(String name, ValueType... types) {
        this.name = name;
        this.types = Lists.newArrayList(types);
    }
}
