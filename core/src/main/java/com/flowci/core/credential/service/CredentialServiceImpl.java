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

import com.flowci.core.common.helper.CipherHelper;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.credential.dao.CredentialDao;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.RSACredential;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author yang
 */
@Log4j2
@Service
public class CredentialServiceImpl implements CredentialService {

    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private SessionManager sessionManager;

    @Override
    public List<Credential> list() {
        return credentialDao.findAll(Sort.by("createdAt"));
    }

    @Override
    public List<Credential> listName() {
        return credentialDao.listNameOnly();
    }

    @Override
    public Credential get(String name) {
        Credential c = credentialDao.findByName(name);

        if (Objects.isNull(c)) {
            throw new NotFoundException("Credential {0} is not found", name);
        }

        return c;
    }

    @Override
    public Credential delete(String name) {
        Credential c = get(name);
        credentialDao.delete(c);
        return c;
    }

    @Override
    public RSACredential createRSA(String name) {
        String email = sessionManager.get().getEmail();
        SimpleKeyPair pair = CipherHelper.RSA.gen(email);

        RSACredential rsaCredential = new RSACredential();
        rsaCredential.setName(name);
        rsaCredential.setPair(pair);

        return save(rsaCredential);
    }

    @Override
    public RSACredential createRSA(String name, SimpleKeyPair pair) {
        RSACredential rsaCredential = new RSACredential();
        rsaCredential.setName(name);
        rsaCredential.setPair(pair);
        return save(rsaCredential);
    }

    private RSACredential save(RSACredential keyPair) {
        try {
            Date now = Date.from(Instant.now());
            keyPair.setUpdatedAt(now);
            keyPair.setCreatedAt(now);
            keyPair.setCreatedBy(sessionManager.getUserId());
            return credentialDao.insert(keyPair);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Credential name {0} is already defined", keyPair.getName());
        }
    }
}
