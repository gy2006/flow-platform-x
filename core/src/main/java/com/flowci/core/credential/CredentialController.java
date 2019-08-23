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

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.common.helper.CipherHelper;
import com.flowci.core.credential.domain.CreateRSA;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.CredentialAction;
import com.flowci.core.credential.domain.GenRSA;
import com.flowci.core.credential.service.CredentialService;
import com.flowci.domain.SimpleKeyPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author yang
 */
@RestController
@RequestMapping("/credentials")
public class CredentialController {

    @Autowired
    private CredentialService credentialService;

    @GetMapping("/{name}")
    @Action(CredentialAction.GET)
    public Credential getByName(@PathVariable String name) {
        return credentialService.get(name);
    }

    @GetMapping
    @Action(CredentialAction.LIST)
    public List<Credential> list() {
        return credentialService.list();
    }

    @GetMapping("/list/name")
    @Action(CredentialAction.LIST_NAME)
    public List<Credential> listName() {
        return credentialService.listName();
    }

    @PostMapping("/rsa")
    @Action(CredentialAction.CREATE_RSA)
    public Credential create(@Validated @RequestBody CreateRSA body) {
        if (body.hasKeyPair()) {
            return credentialService.createRSA(body.getName(), body.getKeyPair());
        }

        return credentialService.createRSA(body.getName());
    }

    @PostMapping("/rsa/gen")
    @Action(CredentialAction.GENERATE_RSA)
    public SimpleKeyPair genByEmail(@Validated @RequestBody GenRSA body) {
        return CipherHelper.genRsa(body.getEmail());
    }

    @DeleteMapping("/{name}")
    @Action(CredentialAction.DELETE)
    public Credential delete(@PathVariable String name) {
        return credentialService.delete(name);
    }
}
