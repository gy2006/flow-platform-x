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

import com.flowci.domain.SimpleKeyPair;
import com.flowci.exception.StatusException;
import com.flowci.util.StringHelper;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public abstract class CipherHelper {

    public static abstract class RSA {

        private final static String RsaPrivateKeyStart = "-----BEGIN RSA PRIVATE KEY-----";

        private final static String RsaPrivateKeyEnd = "-----END RSA PRIVATE KEY-----";

        public static boolean isPrivateKey(String src) {
            src = src.trim();
            return src.startsWith(RsaPrivateKeyStart) && src.endsWith(RsaPrivateKeyEnd);
        }

        public static SimpleKeyPair gen(String email) {
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

    public static abstract class AES {

        public static String encrypt(String source, String secret) {
            try {
                SecretKeySpec key = new SecretKeySpec(toBytes(secret), "AES");
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
                cipher.init(Cipher.ENCRYPT_MODE, key);

                byte[] bytes = cipher.doFinal(toBytes(source));
                return Base64.getEncoder().encodeToString(bytes);
            } catch (Throwable e) {
                return StringHelper.EMPTY;
            }
        }

        public static String decrypt(String encrypted, String secret) {
            try {
                SecretKeySpec key = new SecretKeySpec(toBytes(secret), "AES");

                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
                cipher.init(Cipher.DECRYPT_MODE, key);

                byte[] source = cipher.doFinal(Base64.getDecoder().decode(encrypted));
                return new String(source);
            } catch (Throwable e) {
                return StringHelper.EMPTY;
            }
        }
    }

    private static byte[] toBytes(String val) {
        return val.getBytes(StandardCharsets.UTF_8);
    }
}
