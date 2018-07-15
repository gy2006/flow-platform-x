/*
 * Copyright 2018 fir.im
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

/**
 * @author yang
 */
public class CIException extends RuntimeException {

    @Getter
    private final Integer code;

    public CIException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    public CIException(String message, Throwable cause, Integer code) {
        super(message, cause);
        this.code = code;
    }
}
