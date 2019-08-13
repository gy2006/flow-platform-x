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

package com.flowci.core.test.flow;

import com.flowci.core.flow.dao.FlowUserListDao;
import com.flowci.core.flow.domain.FlowUser;
import com.flowci.core.test.SpringScenario;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class FlowUserListDaoTest extends SpringScenario {

    @Autowired
    private FlowUserListDao flowUserListDao;

    @Test
    public void should_insert_unique_user_id() {
        flowUserListDao.create("123");

        FlowUser user1 = new FlowUser("1");
        FlowUser user2 = new FlowUser("2");

        boolean inserted = flowUserListDao.insert("123", user1, user2);
        Assert.assertTrue(inserted);
    }
}
