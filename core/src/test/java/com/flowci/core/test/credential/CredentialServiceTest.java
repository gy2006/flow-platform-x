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

package com.flowci.core.test.credential;

import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.RSAKeyPair;
import com.flowci.core.credential.service.CredentialService;
import com.flowci.core.test.SpringScenario;
import com.flowci.exception.DuplicateException;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class CredentialServiceTest extends SpringScenario {

    @Autowired
    private CredentialService credentialService;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_create_rsa_credential() {
        Credential rsa = credentialService.createRSA("hello.rsa");
        Assert.assertNotNull(rsa);
        Assert.assertEquals(Credential.Category.SSH_RSA, rsa.getCategory());
        Assert.assertEquals(authManager.getUserId(), rsa.getCreatedBy());
        Assert.assertNotNull(rsa.getCreatedAt());
        Assert.assertNotNull(rsa.getUpdatedAt());

        Credential loaded = credentialService.get("hello.rsa");
        Assert.assertTrue(loaded instanceof RSAKeyPair);

        RSAKeyPair keyPair = (RSAKeyPair) loaded;
        Assert.assertNotNull(keyPair.getPublicKey());
        Assert.assertNotNull(keyPair.getPrivateKey());
    }

    @Test(expected = DuplicateException.class)
    public void should_throw_duplicate_error_on_same_name() {
        credentialService.createRSA("hello.rsa");
        credentialService.createRSA("hello.rsa");
    }

    @Test
    public void should_list_credential() {
        credentialService.createRSA("hello.rsa.1");
        credentialService.createRSA("hello.rsa.2");

        List<Credential> list = credentialService.list();
        Assert.assertEquals(2, list.size());

        Assert.assertEquals("hello.rsa.1", list.get(0).getName());
        Assert.assertEquals("hello.rsa.2", list.get(1).getName());

        List<Credential> names = credentialService.listName();
        Assert.assertEquals(2, names.size());

        Assert.assertEquals("hello.rsa.1", names.get(0).getName());
        Assert.assertEquals("hello.rsa.2", names.get(1).getName());
    }
}
