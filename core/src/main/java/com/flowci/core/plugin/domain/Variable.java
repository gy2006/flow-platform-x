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

package com.flowci.core.plugin.domain;

import com.flowci.domain.VarType;
import com.google.common.base.Strings;
import java.io.Serializable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author yang
 */
@Getter
@Setter
@ToString(of = {"name"})
@EqualsAndHashCode(of = {"name"})
@NoArgsConstructor
@Accessors(chain = true)
public class Variable implements Serializable {

    private String name;

    private String alias;

    private VarType type = VarType.STRING;

    private boolean required = true;

    public Variable(String name) {
        this.name = name;
    }

    public Variable(String name, VarType type) {
        this.name = name;
        this.type = type;
    }

    public boolean verify(String value) {
        if (required && Strings.isNullOrEmpty(value)) {
            return false;
        }

        if (!required && Strings.isNullOrEmpty(value)) {
            return true;
        }

        return VarType.verify(type, value);
    }
}
