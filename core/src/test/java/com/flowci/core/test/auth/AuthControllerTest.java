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

import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.user.domain.User;
import com.flowci.domain.http.ResponseMessage;
import com.flowci.exception.AuthenticationException;
import com.flowci.exception.ErrorCode;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

public class AuthControllerTest extends SpringScenario {

    private User user;

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private CacheManager authCacheManager;

    @Before
    public void createUser() {
        authHelper.enableAuth();

        CaffeineCacheManager cacheManager = (CaffeineCacheManager) this.authCacheManager;
        cacheManager.setCaffeineSpec(CaffeineSpec.parse("expireAfterWrite=2s"));

        user = userService.create("test@flow.ci", "12345", User.Role.Admin);
    }

    @Test(expected = AuthenticationException.class)
    public void should_login_and_logout_successfully() throws Exception {
        // init: log in
        ResponseMessage<String> message = authHelper.login(user.getEmail(), user.getPasswordOnMd5());
        String token = message.getData();

        Assert.assertEquals(user, authService.get());
        Assert.assertTrue(authService.set(token));

        // when: request logout
        ResponseMessage logoutMsg = authHelper.logout(token);
        Assert.assertEquals(StatusCode.OK, logoutMsg.getCode());

        // then: should throw new AuthenticationException("Not logged in") exception
        Assert.assertFalse(authService.set(token));
        authService.get();
    }

    @Test
    public void should_login_and_return_401_with_invalid_password() throws Exception {
        ResponseMessage<String> message = authHelper.login(user.getEmail(), "wrong..");

        Assert.assertEquals(ErrorCode.AUTH_FAILURE, message.getCode());
        Assert.assertEquals("Invalid password", message.getMessage());
    }

    @Test
    public void should_login_and_token_expired() throws Exception {
        // init: log in
        ResponseMessage<String> message = authHelper.login(user.getEmail(), user.getPasswordOnMd5());
        String token = message.getData();

        // when: wait for expire
        ThreadHelper.sleep(5000);

        // then: token should be expired
        Assert.assertFalse(authService.set(token));
    }

    @Test
    public void should_refresh_token() throws Exception {
        ResponseMessage<String> message = authHelper.login(user.getEmail(), user.getPasswordOnMd5());
        String token = message.getData();

        // when:
        ResponseMessage<String> refreshed = authHelper.refresh(token);

        // then:
        Assert.assertNotEquals(token, refreshed.getData());
        Assert.assertTrue(authService.set(refreshed.getData()));
        Assert.assertEquals(user, authService.get());
    }
}
