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

package com.flowci.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import lombok.Data;

/**
 * @author yang
 */
public class CipherHelper {

    @Data
    public static class StringKeyPair {

        private String publicKey;

        private String privateKey;
    }

    public static Path writeToFile(StringKeyPair pair, Path dir, String fileName) throws IOException {
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path privateKeyPath = Paths.get(dir.toString(), fileName);
        Files.deleteIfExists(privateKeyPath);
        try (BufferedWriter writer = Files.newBufferedWriter(privateKeyPath, StandardOpenOption.CREATE_NEW)) {
            writer.write(pair.privateKey);
        }

        Path publicKeyPath = Paths.get(dir.toString(), fileName + ".pub");
        Files.deleteIfExists(publicKeyPath);
        try (BufferedWriter writer = Files.newBufferedWriter(publicKeyPath, StandardOpenOption.CREATE_NEW)) {
            writer.write(pair.publicKey);
        }

        return dir;
    }

    public static class RSA {

        public static final int SIZE_1024 = 1024;

        public static final int SIZE_2048 = 1024;

        public static KeyPair buildKeyPair(int keySize) throws NoSuchAlgorithmException {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize, new SecureRandom());
            return keyPairGenerator.genKeyPair();
        }

        public static StringKeyPair encodeAsOpenSSH(KeyPair pair, String user) throws IOException {
            StringKeyPair stringKeyPair = new StringKeyPair();
            stringKeyPair.publicKey = encodeAsOpenSSH((RSAPublicKey) pair.getPublic(), user);
            stringKeyPair.privateKey = "-----BEGIN RSA PRIVATE KEY-----"
                + "\n"
                + Base64.getMimeEncoder().encodeToString(pair.getPrivate().getEncoded())
                + "\n"
                + "-----END RSA PRIVATE KEY-----";
            return stringKeyPair;
        }

        /**
         * http://www.ietf.org/rfc/rfc4253.txt
         *
         * The "ssh-rsa" key format has the following specific encoding:
         *  string    "ssh-rsa"
         *  mpint     e
         *  mpint     n
         */
        private static String encodeAsOpenSSH(RSAPublicKey pk, String user) throws IOException {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] data = new byte[]{0, 0, 0, 7, 's', 's', 'h', '-', 'r', 's', 'a'};
                out.write(data);

                /* Encode the public exponent */
                byte[] bytesForPublicExponent = pk.getPublicExponent().toByteArray();
                encodeUInt32(bytesForPublicExponent.length, out);
                out.write(bytesForPublicExponent);

                /* Encode the modulus */
                byte[] bytesForModulus = pk.getModulus().toByteArray();
                encodeUInt32(bytesForModulus.length, out);
                out.write(bytesForModulus);

                String publicKeyEncoded = new String(Base64.getEncoder().encode(out.toByteArray()));
                return "ssh-rsa " + publicKeyEncoded + " " + user;
            }
        }

        private static void encodeUInt32(int value, OutputStream out) throws IOException {
            byte[] tmp = new byte[4];
            tmp[0] = (byte) ((value >>> 24) & 0xff);
            tmp[1] = (byte) ((value >>> 16) & 0xff);
            tmp[2] = (byte) ((value >>> 8) & 0xff);
            tmp[3] = (byte) (value & 0xff);
            out.write(tmp);
        }

        private RSA() {
        }
    }

    private CipherHelper() {
    }
}
