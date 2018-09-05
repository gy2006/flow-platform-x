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

package com.flowci.core.adviser;

import com.flowci.domain.http.ResponseMessage;
import com.flowci.core.domain.StatusCode;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * @author yang
 */
@ControllerAdvice({
    "com.flowci.core.flow",
    "com.flowci.core.job",
    "com.flowci.core.agent"
})
public class ResponseMessageAdviser implements ResponseBodyAdvice {

    private final static String SUCCESS_MESSAGE = "success";

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        if (body instanceof InputStreamResource) {
            return body;
        }

        if (MediaType.TEXT_PLAIN.equals(selectedContentType)) {
            return body;
        }

        return new ResponseMessage<>(StatusCode.OK, SUCCESS_MESSAGE, body);
    }
}
