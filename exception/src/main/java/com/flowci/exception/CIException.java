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

package com.flowci.exception;


import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static java.text.MessageFormat.format;

/**
 * @author yang
 */
@Getter
@Setter
@Accessors(chain = true)
public class CIException extends RuntimeException {

    private Object extra;

    public CIException(final String message, final String... params) {
        super(format(message, params));
    }

    public CIException(final String message, final Throwable cause, final String... params) {
        super(format(message, params), cause);
    }

    public Integer getCode() {
        return ErrorCode.ERROR;
    }
}
