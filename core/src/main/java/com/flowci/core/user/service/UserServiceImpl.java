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

package com.flowci.core.user.service;

import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.user.dao.UserDao;
import com.flowci.core.user.domain.User;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.util.HashingHelper;
import java.util.Objects;
import javax.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private ConfigProperties.Admin adminProperties;

    @Autowired
    private UserDao userDao;

    @Autowired
    private SessionManager sessionManager;

    @PostConstruct
    public void initAdmin() {
        String adminEmail = adminProperties.getDefaultEmail();
        String adminPassword = adminProperties.getDefaultPassword();

        try {
            create(adminEmail, adminPassword, User.Role.Admin);
            log.info("Admin {} been initialized", adminEmail);
        } catch (DuplicateException ignore) {

        }
    }

    @Override
    public Page<User> list(Pageable pageable) {
        return userDao.findAll(pageable);
    }

    @Override
    public User defaultAdmin() {
        String email = adminProperties.getDefaultEmail();
        return userDao.findByEmail(email);
    }

    @Override
    public User create(String email, String password, User.Role role) {
        try {
            User user = new User(email, HashingHelper.md5(password));
            user.setRole(role);
            return userDao.insert(user);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Email {0} is already existed", email);
        }
    }

    @Override
    public User getByEmail(String email) {
        return userDao.findByEmail(email);
    }

    @Override
    public void changePassword(String oldOnMd5, String newOnMd5) {
        User user = sessionManager.get();

        if (Objects.equals(user.getPasswordOnMd5(), oldOnMd5)) {
            user.setPasswordOnMd5(newOnMd5);
            userDao.save(user);
            return;
        }

        throw new ArgumentException("The password is incorrect");
    }
}
