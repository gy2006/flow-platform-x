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

package com.flowci.core.open.credential;

import com.flowci.core.credential.dao.CredentialDao;
import com.flowci.core.credential.domain.Credential;
import com.flowci.exception.NotFoundException;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class OpenCredentialService {

    @Autowired
    private CredentialDao credentialDao;

    public Credential get(String name, Class<? extends Credential> target) {
        Credential credential = credentialDao.findByName(name);

        if (Objects.isNull(credential)) {
            throw new NotFoundException("Credential {0} is not found", name);
        }

        if (credential.getClass().equals(target)) {
            return credential;
        }

        throw new NotFoundException("Credential {0} is not found", name);
    }

}
