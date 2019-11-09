/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.credential.domain;

import com.flowci.domain.SimpleAuthPair;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * @author yang
 */
@Data
public class CreateAuth {

    @NotNull
    private String name;

    @NotNull
    private String username;

    @NotNull
    private String password;

    public SimpleAuthPair getAuthPair() {
        return SimpleAuthPair.of(username, password);
    }
}
