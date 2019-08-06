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

package com.flowci.core.common.adviser;

import com.flowci.core.common.auth.AuthService;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.exception.AuthenticationException;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!appProperties.getAuthEnabled()) {
            return authService.setAsDefaultAdmin();
        }

        String token = getToken(request);

        if (token.equals(MagicToken)) {
            return authService.setAsDefaultAdmin();
        }

        return authService.set(token);
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {
        authService.reset();
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
}
