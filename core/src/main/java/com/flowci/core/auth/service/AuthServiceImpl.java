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

package com.flowci.core.auth.service;

import com.flowci.core.auth.annotation.Action;
import com.flowci.core.auth.config.AuthConfig;
import com.flowci.core.auth.domain.PermissionMap;
import com.flowci.core.auth.helper.JwtHelper;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.user.domain.User;
import com.flowci.core.user.service.UserService;
import com.flowci.exception.AuthenticationException;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private ThreadLocal<User> currentUser;

    @Autowired
    private ConfigProperties.Auth authProperties;

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager authCacheManager;

    @Autowired
    private PermissionMap permissionMap;

    @Override
    public Boolean isEnabled() {
        return authProperties.getEnabled();
    }

    @Override
    public User get() {
        User user = currentUser.get();
        if (!hasLogin()) {
            throw new AuthenticationException("Not logged in");
        }
        return user;
    }

    @Override
    public String getUserId() {
        return get().getId();
    }

    @Override
    public String login(String email, String passwordOnMd5) {
        User user = userService.getByEmail(email);

        if (Objects.isNull(user)) {
            throw new AuthenticationException("Invalid email");
        }

        if (!Objects.equals(user.getPasswordOnMd5(), passwordOnMd5)) {
            throw new AuthenticationException("Invalid password");
        }

        String token = JwtHelper.create(user, authProperties.getExpireSeconds());
        currentUser.set(user);
        getOnlineCache().put(email, user);
        return token;
    }

    @Override
    public void logout() {
        User user = get();
        getOnlineCache().evict(user.getEmail());
    }

    @Override
    public boolean hasPermission(Action action) {
        // everyone has permission if no action defined
        if (Objects.isNull(action)) {
            return true;
        }

        // admin has all permission
        User user = get();
        return permissionMap.hasPermission(user.getRole(), action.value());
    }

    @Override
    public boolean hasLogin() {
        return currentUser.get() != null;
    }

    @Override
    public String refresh(String token) {
        String email = JwtHelper.decode(token);

        User user = getOnlineCache().get(email, User.class);
        if (Objects.isNull(user)) {
            throw new AuthenticationException("Invalid token");
        }

        boolean verify = JwtHelper.verify(token, user);
        if (verify) {
            return JwtHelper.create(user, authProperties.getExpireSeconds());
        }

        throw new AuthenticationException("Invalid token");
    }

    @Override
    public boolean set(String token) {
        String email = JwtHelper.decode(token);

        User user = getOnlineCache().get(email, User.class);
        if (Objects.isNull(user)) {
            return false;
        }

        boolean verify = JwtHelper.verify(token, user);
        if (verify) {
            currentUser.set(user);
            return true;
        }

        return false;
    }

    @Override
    public boolean set(User user) {
        currentUser.set(user);
        return true;
    }

    @Override
    public boolean setAsDefaultAdmin() {
        User defaultAdmin = userService.defaultAdmin();
        currentUser.set(defaultAdmin);
        return true;
    }

    @Override
    public void reset() {
        currentUser.set(null);
    }

    private Cache getOnlineCache() {
        return authCacheManager.getCache(AuthConfig.CACHE_ONLINE);
    }
}
