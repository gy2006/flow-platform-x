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

package com.flowci.core.auth.service;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.user.domain.User;

/**
 * 'login' ->
 * 'set' per request ->
 * 'get' current user | 'hasLogin' | 'getUserId' | 'refresh' ->
 * 'logout'
 */
public interface AuthService {

    Boolean isEnabled();

    /**
     * Get current logged in user
     */
    User get();

    /**
     * Get current logged in user id
     */
    String getUserId();

    /**
     * Login and return jwt token
     */
    String login(String email, String passwordOnMd5);

    /**
     * Logout from current user
     */
    void logout();

    /**
     * Refresh and return new token
     */
    String refresh(String token);

    /**
     * Check current user has access right to given action
     * @param action can be null which means no permission control
     */
    boolean hasPermission(Action action);

    /**
     * Check is logged in
     */
    boolean hasLogin();

    /**
     * Set current user by token
     * @return Current user object or null if not verified
     */
    boolean set(String token);

    /**
     * Set current user by instance
     */
    boolean set(User user);

    /**
     * Set current user from default admin form config properties
     */
    boolean setAsDefaultAdmin();

    /**
     * Remove current logged in user
     */
    void reset();
}
