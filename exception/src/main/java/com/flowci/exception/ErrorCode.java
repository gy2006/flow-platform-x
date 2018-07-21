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

/**
 * @author yang
 */
public final class ErrorCode {

    public final static int ERROR = 400;

    public final static int INVALID_ARGUMENT = 401;

    public static final int PARSE_YML = 402;

    public static final int DUPLICATE = 403;

    public static final int NOT_FOUND = 404;

    private ErrorCode() {

    }
}
