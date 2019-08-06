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
import com.flowci.core.auth.JwtHelper;
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.test.MvcMockHelper;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.user.domain.User;
import com.flowci.domain.http.ResponseMessage;
import com.flowci.exception.ErrorCode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Base64;

public class AuthControllerTest extends SpringScenario {

    private final TypeReference<ResponseMessage<String>> loginType =
            new TypeReference<ResponseMessage<String>>() {
            };

    private User user;

    @Autowired
    private MvcMockHelper mvcMockHelper;

    @Before
    public void createUser() {
        user = userService.create("test@flow.ci", "12345", User.Role.Admin);
    }

    @Test
    public void should_login_and_return_jwt_token() throws Exception {
        // when: send login request
        MockHttpServletRequestBuilder builder = buildLoginRequest(user.getEmail(), user.getPasswordOnMd5());

        ResponseMessage<String> message = mvcMockHelper.expectSuccessAndReturnClass(builder, loginType);
        Assert.assertEquals(StatusCode.OK, message.getCode());

        // then:
        String token = message.getData();
        Assert.assertNotNull(token);
        Assert.assertTrue(JwtHelper.verify(token, user));
    }

    @Test
    public void should_login_and_return_401_with_invalid_password() throws Exception {
        MockHttpServletRequestBuilder builder = buildLoginRequest(user.getEmail(), "wrong..");

        ResponseMessage<String> message = mvcMockHelper.expectSuccessAndReturnClass(builder, loginType);
        Assert.assertEquals(ErrorCode.AUTH_FAILURE, message.getCode());
        Assert.assertEquals("Invalid password", message.getMessage());
    }

    private MockHttpServletRequestBuilder buildLoginRequest(String email, String passwordOnMd5) {
        String authContent = email + ":" + passwordOnMd5;
        String base64Content = Base64.getEncoder().encodeToString(authContent.getBytes());

        return MockMvcRequestBuilders.post("auth").header("Authorization", "Basic " + base64Content);
    }
}
