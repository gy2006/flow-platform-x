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

package com.flowci.core.common.manager;

import com.flowci.core.user.domain.User;
import com.flowci.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SessionManager {

    private final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public User get() {
        User user = currentUser.get();
        if (!exist()) {
            throw new AuthenticationException("Not logged in");
        }
        return user;
    }

    public String getUserId() {
        return get().getId();
    }

    public void set(User user) {
        currentUser.set(user);
    }

    public User remove() {
        User user = currentUser.get();
        currentUser.remove();
        return user;
    }

    public boolean exist() {
        return currentUser.get() != null;
    }
}
