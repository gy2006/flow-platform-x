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

package com.flowci.core.common.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.flowci.core.user.domain.User;
import com.flowci.util.StringHelper;

import java.time.Instant;
import java.util.Date;

/**
 * @author yang
 */
public class JwtHelper {

    private static final String issuer = "flow.ci";

    public static String create(User user, int expiredAfterSeconds) {
        Algorithm algorithm = Algorithm.HMAC256(user.getPasswordOnMd5());
        Instant expired = Instant.now().plusSeconds(expiredAfterSeconds);

        return JWT.create()
                .withIssuer(issuer)
                .withIssuedAt(Date.from(Instant.now()))
                .withJWTId(user.getEmail())
                .withClaim("role", user.getRole().toString())
                .withExpiresAt(Date.from(expired))
                .sign(algorithm);
    }

    /**
     * Decode token and return user email
     */
    public static String decode(String token) {
        try {
            DecodedJWT decode = JWT.decode(token);
            return decode.getId();
        } catch (JWTDecodeException e) {
            return StringHelper.EMPTY;
        }
    }

    public static boolean verify(String token, User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(user.getPasswordOnMd5());
            JWTVerifier verifier = JWT.require(algorithm).withIssuer(issuer).build();

            DecodedJWT decoded = verifier.verify(token);
            return decoded.getExpiresAt().before(Date.from(Instant.now()));
        } catch (JWTVerificationException e) {
            return false;
        }
    }
}
