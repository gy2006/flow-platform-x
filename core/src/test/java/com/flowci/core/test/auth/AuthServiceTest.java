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

import com.flowci.core.auth.JwtHelper;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.user.domain.User;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AuthServiceTest extends SpringScenario {

    private User user;

    @Before
    public void createUser() {
        user = userService.create("test@flow.ci", "12345", User.Role.Admin);
    }

    @Test
    public void should_login_and_return_jwt_token() {
        String token = authService.login(user.getEmail(), user.getPasswordOnMd5());
        Assert.assertNotNull(token);

        String email = JwtHelper.decode(token);
        Assert.assertEquals(user.getEmail(), email);
    }
}
