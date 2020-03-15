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
import com.flowci.core.credential.domain.CreateAuth;
import com.flowci.core.credential.domain.CreateRSA;
import com.flowci.core.credential.domain.Secret;
import com.flowci.core.credential.domain.SecretAction;
import com.flowci.core.credential.service.SecretService;
import com.flowci.domain.SimpleKeyPair;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/credentials")
public class SecretController {

    @Autowired
    private SecretService credentialService;

    @GetMapping("/{name}")
    @Action(SecretAction.GET)
    public Secret getByName(@PathVariable String name) {
        return credentialService.get(name);
    }

    @GetMapping
    @Action(SecretAction.LIST)
    public List<Secret> list() {
        return credentialService.list();
    }

    @GetMapping("/list/name")
    @Action(SecretAction.LIST_NAME)
    public List<Secret> listName(@RequestParam String category) {
        return credentialService.listName(category);
    }

    @PostMapping("/rsa")
    @Action(SecretAction.CREATE_RSA)
    public Secret create(@Validated @RequestBody CreateRSA body) {
        if (body.hasKeyPair()) {
            return credentialService.createRSA(body.getName(), body.getKeyPair());
        }

        return credentialService.createRSA(body.getName());
    }

    @PostMapping("/auth")
    @Action(SecretAction.CREATE_AUTH)
    public Secret create(@Validated @RequestBody CreateAuth body) {
        return credentialService.createAuth(body.getName(), body.getAuthPair());
    }

    @PostMapping("/rsa/gen")
    @Action(SecretAction.GENERATE_RSA)
    public SimpleKeyPair genByEmail() {
        return credentialService.genRSA();
    }

    @DeleteMapping("/{name}")
    @Action(SecretAction.DELETE)
    public Secret delete(@PathVariable String name) {
        return credentialService.delete(name);
    }
}
