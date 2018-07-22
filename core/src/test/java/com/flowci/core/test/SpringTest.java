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

package com.flowci.core.test;

import com.flowci.core.test.SpringTest.Config;
import com.flowci.core.user.User;
import com.flowci.core.user.UserService;
import java.io.InputStream;
import java.util.Objects;
import lombok.Getter;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author yang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Config.class)
@AutoConfigureMockMvc
public abstract class SpringTest {

    @TestConfiguration
    public static class Config {

        @Bean("mvcMockHelper")
        public MvcMockHelper mvcMockHelper() {
            return new MvcMockHelper();
        }
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Getter
    @Autowired
    private ThreadLocal<User> currentUser;

    @Autowired
    private UserService userService;

    @After
    public void dbClean() {
        mongoTemplate.getDb().drop();
    }

    InputStream load(String resource) {
        return SpringTest.class.getClassLoader().getResourceAsStream(resource);
    }

    void mockLogin() {
        User user = userService.getByEmail("test@flow.ci");
        if (Objects.isNull(user)) {
            user = userService.create("test@flow.ci", "12345");
        }
        currentUser.set(user);
    }

}
