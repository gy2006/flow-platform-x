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

package com.flowci.core.credential;

import com.flowci.core.credential.domain.CreateCredential;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.service.CredentialService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/credentials")
public class CredentialController {

    @Autowired
    private CredentialService credentialService;

    @GetMapping("/{name}")
    public Credential getByName(@PathVariable String name) {
        return credentialService.get(name);
    }

    @GetMapping
    public List<Credential> list(){
        return credentialService.list();
    }

    @PostMapping("/rsa")
    public Credential create(@Validated @RequestBody CreateCredential create) {
        return credentialService.createRSA(create.getName());
    }

}
