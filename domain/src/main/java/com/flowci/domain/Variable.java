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

import com.flowci.util.ObjectsHelper;
import com.flowci.util.PatternHelper;
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

    public enum ValueType {

        STRING,

        INTEGER,

        HTTP_URL,

        GIT_URL,

        EMAIL
    }

    private String name;

    private String alias;

    private ValueType type = ValueType.STRING;

    private boolean required = true;

    public Variable(String name) {
        this.name = name;
    }

    public Variable(String name, ValueType type) {
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

        return isTypeMatch(type, value);
    }

    private static boolean isTypeMatch(ValueType type, String value) {
        switch (type) {
            case INTEGER:
                return ObjectsHelper.tryParseInt(value);

            case HTTP_URL:
                return PatternHelper.WEB_URL.matcher(value).find();

            case GIT_URL:
                return PatternHelper.GIT_URL.matcher(value).find();

            case EMAIL:
                return PatternHelper.EMAIL_ADDRESS.matcher(value).find();
        }

        return true;
    }
}
