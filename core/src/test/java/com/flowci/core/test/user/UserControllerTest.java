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

package com.flowci.core.test.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.test.MockMvcHelper;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.user.domain.ChangePassword;
import com.flowci.core.user.domain.CreateUser;
import com.flowci.core.user.domain.User;
import com.flowci.domain.http.ResponseMessage;
import com.flowci.exception.AuthenticationException;
import com.flowci.exception.ErrorCode;
import com.flowci.util.HashingHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class UserControllerTest extends SpringScenario {

    private static final TypeReference<ResponseMessage<User>> UserType =
            new TypeReference<ResponseMessage<User>>() {
            };

    @Autowired
    private MockMvcHelper mockMvcHelper;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void loginBefore() {
        mockLogin();
    }

    @Test(expected = AuthenticationException.class)
    public void should_change_password_for_current_user_successfully() throws Exception {
        User user = sessionManager.get();

        String newPwOnMd5 = HashingHelper.md5("11111");
        ChangePassword body = new ChangePassword();
        body.setOld(user.getPasswordOnMd5());
        body.setNewOne(newPwOnMd5);
        body.setConfirm(newPwOnMd5);

        // when:
        ResponseMessage message = mockMvcHelper.expectSuccessAndReturnClass(
                post("/users/change/password")
                        .content(objectMapper.writeValueAsBytes(body))
                        .contentType(MediaType.APPLICATION_JSON), ResponseMessage.class);

        Assert.assertEquals(StatusCode.OK, message.getCode());

        // then:
        Assert.assertEquals(newPwOnMd5, userService.getByEmail(user.getEmail()).getPasswordOnMd5());

        // then: throw AuthenticationException
        sessionManager.get();
    }

    @Test
    public void should_create_user_successfully() throws Exception {
        CreateUser body = new CreateUser();
        body.setEmail("test@flow.ci");
        body.setPasswordOnMd5(HashingHelper.md5("111111"));
        body.setRole(User.Role.Admin.name());

        mockMvcHelper.expectSuccessAndReturnClass(post("/users")
                .content(objectMapper.writeValueAsBytes(body))
                .contentType(MediaType.APPLICATION_JSON), UserType);

        User created = userService.getByEmail("test@flow.ci");
        Assert.assertEquals(User.Role.Admin, created.getRole());
        Assert.assertNotNull(created.getCreatedBy());
        Assert.assertNotNull(created.getCreatedAt());
        Assert.assertNotNull(created.getUpdatedAt());
    }

    @Test
    public void should_fail_to_create_user_if_mail_invalid() throws Exception {
        CreateUser body = new CreateUser();
        body.setEmail("test.flow.ci");
        body.setPasswordOnMd5(HashingHelper.md5("111111"));
        body.setRole(User.Role.Admin.name());

        ResponseMessage message = mockMvcHelper.expectSuccessAndReturnClass(post("/users")
                .content(objectMapper.writeValueAsBytes(body))
                .contentType(MediaType.APPLICATION_JSON), ResponseMessage.class);

        Assert.assertEquals(ErrorCode.INVALID_ARGUMENT, message.getCode());
    }
}
