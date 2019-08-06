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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.test.MvcMockHelper;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.user.domain.User;
import com.flowci.domain.http.ResponseMessage;
import com.flowci.exception.AuthenticationException;
import com.flowci.exception.ErrorCode;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import java.util.Base64;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class AuthControllerTest extends SpringScenario {

    private final TypeReference<ResponseMessage<String>> loginType =
        new TypeReference<ResponseMessage<String>>() {
        };

    private User user;

    @Autowired
    private MvcMockHelper mvcMockHelper;

    @Autowired
    private ConfigProperties.Auth authProperties;

    @Autowired
    private CacheManager authCacheManager;

    @Before
    public void createUser() {
        authProperties.setEnabled(true);

        CaffeineCacheManager cacheManager = (CaffeineCacheManager) this.authCacheManager;
        cacheManager.setCaffeineSpec(CaffeineSpec.parse("expireAfterWrite=2s"));

        user = userService.create("test@flow.ci", "12345", User.Role.Admin);
    }

    @Test(expected = AuthenticationException.class)
    public void should_login_and_logout_successfully() throws Exception {
        // init: log in
        ResponseMessage<String> message = sendLoginRequest(user.getEmail(), user.getPasswordOnMd5());
        String token = message.getData();

        Assert.assertEquals(user, authService.get());
        Assert.assertTrue(authService.set(token));

        // when: request logout
        ResponseMessage logoutMsg = mvcMockHelper.expectSuccessAndReturnClass(
            post("/auth/logout").header("Token", token),
            ResponseMessage.class
        );
        Assert.assertEquals(StatusCode.OK, logoutMsg.getCode());

        // then: should throw new AuthenticationException("Not logged in") exception
        Assert.assertFalse(authService.set(token));
        authService.get();
    }

    @Test
    public void should_login_and_return_401_with_invalid_password() throws Exception {
        ResponseMessage<String> message = sendLoginRequest(user.getEmail(), "wrong..");

        Assert.assertEquals(ErrorCode.AUTH_FAILURE, message.getCode());
        Assert.assertEquals("Invalid password", message.getMessage());
    }

    @Test
    public void should_login_and_token_expired() throws Exception {
        // init: log in
        ResponseMessage<String> message = sendLoginRequest(user.getEmail(), user.getPasswordOnMd5());
        String token = message.getData();

        // when: wait for expire
        ThreadHelper.sleep(5000);

        // then: token should be expired
        Assert.assertFalse(authService.set(token));
    }

    @Test
    public void should_refresh_token() throws Exception {
        ResponseMessage<String> message = sendLoginRequest(user.getEmail(), user.getPasswordOnMd5());
        String token = message.getData();

        // when:
        ResponseMessage<String> refreshed = mvcMockHelper.expectSuccessAndReturnClass(
            post("/auth/refresh").header("Token", token),
            loginType
        );

        // then:
        Assert.assertNotEquals(token, refreshed.getData());
        Assert.assertTrue(authService.set(refreshed.getData()));
        Assert.assertEquals(user, authService.get());
    }

    private ResponseMessage<String> sendLoginRequest(String email, String passwordOnMd5) throws Exception {
        String authContent = email + ":" + passwordOnMd5;
        String base64Content = Base64.getEncoder().encodeToString(authContent.getBytes());
        MockHttpServletRequestBuilder builder = post("/auth/login").header("Authorization", "Basic " + base64Content);

        return mvcMockHelper.expectSuccessAndReturnClass(builder, loginType);
    }
}
