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

package com.flowci.core.adviser;

import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.user.CurrentUserHelper;
import com.flowci.core.user.User;
import com.flowci.core.user.UserService;
import com.flowci.exception.AuthenticationException;
import com.google.common.base.Strings;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author yang
 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final String MagicToken = "helloflowciadmin";

    private static final String HeaderToken = "Token";

    private static final String ParameterToken = "token";

    @Autowired
    private ConfigProperties appProperties;

    @Autowired
    private CurrentUserHelper currentUserHelper;

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!appProperties.getAuthEnabled()) {
            if (Objects.isNull(currentUserHelper.get())) {
                return setAdminToCurrentUser();
            }
            return true;
        }

        currentUserHelper.reset();
        String token = getToken(request);

        if (token.equals(MagicToken)) {
            return setAdminToCurrentUser();
        }

        return true;
    }

    private String getToken(HttpServletRequest request) {
        String token = request.getHeader(HeaderToken);

        if (Strings.isNullOrEmpty(token)) {
            token = request.getParameter(ParameterToken);
        }

        if (Strings.isNullOrEmpty(token)) {
            throw new AuthenticationException("Token is missing");
        }

        return token;
    }

    private boolean setAdminToCurrentUser() {
        User defaultAdmin = userService.defaultAdmin();
        currentUserHelper.set(defaultAdmin);
        return true;
    }
}
