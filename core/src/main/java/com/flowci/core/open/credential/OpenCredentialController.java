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

package com.flowci.core.open.credential;

import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.RSACredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/open/credential")
public class OpenCredentialController {

    @Autowired
    private OpenCredentialService openCredentialService;

    @GetMapping("/rsa/{name}/private")
    public String getRsaPrivateKey(@PathVariable String name) {
        Credential credential = openCredentialService.get(name, RSACredential.class);
        RSACredential pair = (RSACredential) credential;
        return pair.getPrivateKey();
    }
}
