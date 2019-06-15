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

package com.flowci.core.credential.service;

import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.RSAKeyPair;
import com.flowci.util.CipherHelper.StringKeyPair;
import java.util.List;

/**
 * @author yang
 */
public interface CredentialService {

    /**
     * List credential for current user
     */
    List<Credential> list();

    /**
     * Get credential for current user
     */
    Credential get(String name);

    /**
     * Create and auto generate rsa key pair
     */
    RSAKeyPair createRSA(String name);

    /**
     * Create and set rsa key pair
     */
    RSAKeyPair createRSA(String name, StringKeyPair rasKeyPair);

}
