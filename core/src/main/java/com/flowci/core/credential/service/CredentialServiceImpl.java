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

import com.flowci.core.credential.dao.CredentialDao;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.RSAKeyPair;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.StatusException;
import com.flowci.util.CipherHelper.RSA;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class CredentialServiceImpl implements CredentialService {

    @Autowired
    private CredentialDao credentialDao;

    @Override
    public Credential get(String name) {
        return credentialDao.findByName(name);
    }

    @Override
    public Credential createRSA(String name) {
        try {
            return createRSA(name, RSA.buildKeyPair(RSA.SIZE_1024));
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage());
            throw new StatusException("Unable to generate RSA key pair");
        }
    }

    @Override
    public Credential createRSA(String name, KeyPair rasKeyPair) {
        try {
            RSAKeyPair rsaKeyPair = new RSAKeyPair(rasKeyPair);
            rsaKeyPair.setName(name);
            return credentialDao.insert(rsaKeyPair);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Credential name '{}' is already defined", name);
        }
    }
}
