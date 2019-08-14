/*
 * Copyright 2019 fir.im
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

package com.flowci.core.user;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.auth.service.AuthService;
import com.flowci.core.user.domain.ChangePassword;
import com.flowci.core.user.domain.UserAction;
import com.flowci.core.user.service.UserService;
import com.flowci.exception.ArgumentException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @PostMapping("/change/password")
    @Action(UserAction.CHANGE_PASSWORD)
    public void changePassword(@Validated @RequestBody ChangePassword body) {
        if (Objects.equals(body.getNewOne(), body.getConfirm())) {
            userService.changePassword(body.getOld(), body.getNewOne());
            authService.logout();
            return;
        }

        throw new ArgumentException("the confirm password is inconsistent");
    }
}
