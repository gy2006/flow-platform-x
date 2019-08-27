/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.user.service;

import com.flowci.core.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

/**
 * @author yang
 */
public interface UserService {

    /**
     * List all users
     */
    Page<User> list(Pageable pageable);

    /**
     * List users by given ids
     */
    List<User> list(Collection<String> ids);

    /**
     * Get default admin user
     */
    User defaultAdmin();

    /**
     * Create user by email and password;
     */
    User create(String email, String password, User.Role role);

    /**
     * Get user by email
     */
    User getByEmail(String email);

    /**
     * Change password for current user
     */
    void changePassword(String old, String newOne);

    /**
     * Change role for target user
     * @param email target user email
     * @param newRole new role will be change
     */
    void changeRole(String email, User.Role newRole);

    /**
     * Delete user by email
     */
    User delete(String email);
}
