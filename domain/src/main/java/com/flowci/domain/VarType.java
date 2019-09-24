/*
 * Copyright 2019 flow.ci
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

/**
 * @author yang
 */
public enum VarType {

    STRING,

    INT,

    HTTP_URL,

    GIT_URL,

    EMAIL;

    public static boolean verify(VarType type, String value) {
        switch (type) {
            case INT:
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
