/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.common.helper;

import com.flowci.core.common.domain.SimpleKeyPair;
import com.flowci.exception.StatusException;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class CipherHelper {

    private final static String RsaPrivateKeyStart = "-----BEGIN RSA PRIVATE KEY-----";

    private final static String RsaPrivateKeyEnd = "-----END RSA PRIVATE KEY-----";

    public static boolean isRsaPrivateKey(String src) {
        src = src.trim();
        return src.startsWith(RsaPrivateKeyStart) && src.endsWith(RsaPrivateKeyEnd);
    }

    public static SimpleKeyPair genRsa(String email) {
        try (ByteArrayOutputStream pubKeyOS = new ByteArrayOutputStream()) {
            try (ByteArrayOutputStream prvKeyOS = new ByteArrayOutputStream()) {
                JSch jsch = new JSch();
                SimpleKeyPair rsa = new SimpleKeyPair();

                KeyPair kpair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 2048);
                kpair.writePrivateKey(prvKeyOS);
                kpair.writePublicKey(pubKeyOS, email);

                rsa.setPublicKey(pubKeyOS.toString());
                rsa.setPrivateKey(prvKeyOS.toString());

                kpair.dispose();
                return rsa;
            }
        } catch (IOException | JSchException e) {
            throw new StatusException("Unable to generate RSA key pair");
        }
    }
}
