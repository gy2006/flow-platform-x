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

package com.flowci.core.user.domain;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

/**
 * @author yang
 */
@Data
public class CreateUser {

    @NotEmpty
    @Email
    private String email;

    @NotEmpty
    private String passwordOnMd5;

    @NotEmpty
    private String role;

    public User.Role getUserRole() {
        return User.Role.valueOf(role);
    }
}