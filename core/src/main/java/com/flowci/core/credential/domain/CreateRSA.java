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

package com.flowci.core.credential.domain;

import com.flowci.domain.SimpleKeyPair;
import com.google.common.base.Strings;
import javax.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @author yang
 */
@Data
public class CreateRSA {

    @NotEmpty
    private String name;

    @NotEmpty
    private String publicKey;

    @NotEmpty
    private String privateKey;

    public boolean hasKeyPair() {
        return !Strings.isNullOrEmpty(publicKey) && !Strings.isNullOrEmpty(privateKey);
    }

    public SimpleKeyPair getKeyPair() {
        return SimpleKeyPair.of(publicKey, privateKey);
    }

}
