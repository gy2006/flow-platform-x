/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.test.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.test.MvcMockHelper;
import com.flowci.domain.http.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class AuthHelper {

    private final TypeReference<ResponseMessage<String>> loginType =
            new TypeReference<ResponseMessage<String>>() {
            };

    @Autowired
    private MvcMockHelper mvcMockHelper;

    @Autowired
    private ConfigProperties.Auth authProperties;

    public void enableAuth() {
        authProperties.setEnabled(true);
    }

    public void disableAuth() {
        authProperties.setEnabled(false);
    }

    public ResponseMessage<String> login(String email, String passwordOnMd5) throws Exception {
        String authContent = email + ":" + passwordOnMd5;
        String base64Content = Base64.getEncoder().encodeToString(authContent.getBytes());
        MockHttpServletRequestBuilder builder = post("/auth/login").header("Authorization", "Basic " + base64Content);

        return mvcMockHelper.expectSuccessAndReturnClass(builder, loginType);
    }

    public ResponseMessage<String> refresh(String token) throws Exception {
        return mvcMockHelper.expectSuccessAndReturnClass(
                post("/auth/refresh").header("Token", token),
                loginType
        );
    }

    public ResponseMessage logout(String token) throws Exception {
        return mvcMockHelper.expectSuccessAndReturnClass(
                post("/auth/logout").header("Token", token),
                ResponseMessage.class
        );
    }
}