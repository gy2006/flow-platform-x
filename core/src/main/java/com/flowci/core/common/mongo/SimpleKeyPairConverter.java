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

package com.flowci.core.common.mongo;

import com.flowci.core.common.helper.CipherHelper;
import com.flowci.domain.SimpleKeyPair;
import lombok.Getter;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

@Getter
public class SimpleKeyPairConverter {

    private static final String FieldPublicKey = "publicKey";

    private static final String FieldPrivateKey = "privateKey";

    private final String appSecret;

    private final Reader reader;

    private final Writer writer;

    public SimpleKeyPairConverter(String appSecret) {
        this.appSecret = appSecret;
        this.reader = new Reader();
        this.writer = new Writer();
    }

    public class Reader implements Converter<Document, SimpleKeyPair> {

        @Override
        public SimpleKeyPair convert(Document source) {
            String encryptedPublicKey = source.getString(FieldPublicKey);
            String encryptedPrivateKey = source.getString(FieldPrivateKey);

            return SimpleKeyPair.of(
                    CipherHelper.AES.decrypt(encryptedPublicKey, appSecret),
                    CipherHelper.AES.decrypt(encryptedPrivateKey, appSecret)
            );
        }
    }

    public class Writer implements Converter<SimpleKeyPair, Document> {

        @Override
        public Document convert(SimpleKeyPair pair) {
            Document document = new Document();
            document.put(FieldPublicKey, CipherHelper.AES.encrypt(pair.getPublicKey(), appSecret));
            document.put(FieldPrivateKey, CipherHelper.AES.encrypt(pair.getPrivateKey(), appSecret));
            return document;
        }
    }
}
