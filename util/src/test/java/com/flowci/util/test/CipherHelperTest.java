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

package com.flowci.util.test;

import com.flowci.util.CipherHelper;
import com.flowci.util.CipherHelper.RSA;
import com.flowci.util.CipherHelper.StringKeyPair;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author yang
 */
public class CipherHelperTest {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void should_generate_rsa_key_pair_and_save_to_file() throws Throwable {
        KeyPair keyPair = RSA.buildKeyPair(1024);
        Assert.assertNotNull(keyPair);

        StringKeyPair stringKeyPair = RSA.encodeAsOpenSSH(keyPair, "user@flow.ci");
        Assert.assertNotNull(stringKeyPair);
        Assert.assertNotNull(stringKeyPair.getPublicKey());
        Assert.assertNotNull(stringKeyPair.getPrivateKey());

        File rsaDir = folder.newFolder("rsa");
        CipherHelper.writeToFile(stringKeyPair, rsaDir.toPath(), "id_rsa");

        Assert.assertTrue(Files.exists(Paths.get(rsaDir.toString(), "id_rsa")));
        Assert.assertTrue(Files.exists(Paths.get(rsaDir.toString(), "id_rsa.pub")));
    }

}
