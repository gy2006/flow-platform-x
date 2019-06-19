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
import com.flowci.core.user.CurrentUserHelper;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.StatusException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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

    @Autowired
    private CurrentUserHelper currentUserHelper;

    @Override
    public List<Credential> list() {
        return credentialDao.findAllByCreatedByOrderByCreatedAt(currentUserHelper.getUserId());
    }

    @Override
    public Credential get(String name) {
        return credentialDao.findByNameAndCreatedBy(name, currentUserHelper.getUserId());
    }

    @Override
    public RSAKeyPair genRSA(String email) {
        RSAKeyPair keyPair = rsaKeyGen(email);
        if (Objects.isNull(keyPair)) {
            throw new StatusException("Unable to generate RSA key pair");
        }
        return keyPair;
    }

    @Override
    public RSAKeyPair createRSA(String name) {
        String email = currentUserHelper.get().getEmail();
        RSAKeyPair rsaKeyPair = genRSA(email);
        rsaKeyPair.setName(name);
        return save(rsaKeyPair);
    }

    @Override
    public RSAKeyPair createRSA(String name, String publicKey, String privateKey) {
        RSAKeyPair rsaKeyPair = new RSAKeyPair();
        rsaKeyPair.setName(name);
        rsaKeyPair.setPublicKey(publicKey);
        rsaKeyPair.setPrivateKey(privateKey);
        return save(rsaKeyPair);
    }

    private RSAKeyPair save(RSAKeyPair keyPair) {
        try {
            Date now = Date.from(Instant.now());
            keyPair.setUpdatedAt(now);
            keyPair.setCreatedAt(now);
            keyPair.setCreatedBy(currentUserHelper.getUserId());
            return credentialDao.insert(keyPair);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Credential name {0} is already defined", keyPair.getName());
        }
    }

    private RSAKeyPair rsaKeyGen(String email) {
        try (ByteArrayOutputStream pubKeyOS = new ByteArrayOutputStream()) {
            try (ByteArrayOutputStream prvKeyOS = new ByteArrayOutputStream()) {
                JSch jsch = new JSch();
                RSAKeyPair rsa = new RSAKeyPair();

                KeyPair kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048);
                kpair.writePrivateKey(prvKeyOS);
                kpair.writePublicKey(pubKeyOS, email);

                rsa.setPublicKey(pubKeyOS.toString());
                rsa.setPrivateKey(prvKeyOS.toString());

                kpair.dispose();
                return rsa;
            }
        } catch (IOException | JSchException e) {
            log.error(e);
            return null;
        }
    }
}
