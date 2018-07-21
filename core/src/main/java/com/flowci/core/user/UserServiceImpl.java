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

package com.flowci.core.user;

import com.flowci.core.config.ConfigProperties;
import com.flowci.exception.DuplicateException;
import com.flowci.util.HashingHelper;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private ConfigProperties config;

    @Autowired
    private UserDao userDao;

    @PostConstruct
    public void initAdmin() {
        String adminEmail = config.getAdmin().getEmail();
        String adminPassword = config.getAdmin().getPassword();

        create(adminEmail, adminPassword);
        log.info("Admin {} been initialized", adminEmail);
    }

    @Override
    public User create(String email, String password) {
        if (!Objects.isNull(getByEmail(email))) {
            throw new DuplicateException("Email {} is already existed", email);
        }

        new User(email, HashingHelper.md5(password));
        return userDao.save(new User(email, password));
    }

    @Override
    public User getByEmail(String email) {
        return userDao.findByEmail(email);
    }
}
