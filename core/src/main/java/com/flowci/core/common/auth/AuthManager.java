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

package com.flowci.core.common.auth;

import com.flowci.core.user.domain.User;
import com.flowci.exception.CIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthManager {

    @Autowired
    private ThreadLocal<User> currentUser;

    public User get() {
        User user = currentUser.get();
        if (!hasLogin()) {
            throw new CIException("User logged in is required");
        }
        return user;
    }

    public boolean hasLogin() {
        return currentUser.get() != null;
    }

    public String getUserId() {
        return get().getId();
    }

    public void set(User user) {
        currentUser.set(user);
    }

    public void reset() {
        currentUser.set(null);
    }
}
