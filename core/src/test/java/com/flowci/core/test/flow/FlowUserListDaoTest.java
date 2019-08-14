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

import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.FlowUserListDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.FlowUser;
import com.flowci.core.test.SpringScenario;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class FlowUserListDaoTest extends SpringScenario {

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private FlowUserListDao flowUserListDao;

    @Test
    public void should_insert_unique_user_id() {
        Flow flow = createFlow("test-flow");

        FlowUser user1 = new FlowUser("1");
        FlowUser user2 = new FlowUser("2");
        FlowUser userDuplicate = new FlowUser("2");

        boolean inserted = flowUserListDao.insert(flow.getId(), user1, user2, userDuplicate);
        Assert.assertTrue(inserted);

        List<FlowUser> users = flowUserListDao.findAllUsers(flow.getId());
        Assert.assertEquals(2, users.size());
    }

    @Test
    public void should_remove_users() {
        Flow flow = createFlow("test-flow");

        FlowUser user1 = new FlowUser("1");
        FlowUser user2 = new FlowUser("2");
        flowUserListDao.insert(flow.getId(), user1, user2);

        List<FlowUser> users = flowUserListDao.findAllUsers(flow.getId());
        Assert.assertEquals(2, users.size());

        // when: remove user1
        Assert.assertTrue(flowUserListDao.remove(flow.getId(), user1.getUserId()));
        users = flowUserListDao.findAllUsers(flow.getId());
        Assert.assertEquals(1, users.size());

        // then: check existing
        Assert.assertFalse(flowUserListDao.exist(flow.getId(), user1.getUserId()));
        Assert.assertTrue(flowUserListDao.exist(flow.getId(), user2.getUserId()));
    }

    @Test
    public void should_list_flow_ids_by_user() {
        Flow flow1 = createFlow("test-flow-1");
        Flow flow2 = createFlow("test-flow-2");
        Flow flow3 = createFlow("test-flow-3");

        FlowUser user1 = new FlowUser("1");
        FlowUser user2 = new FlowUser("2");

        // when: set user1 for flow 1,2,3, user2 for flow 1
        flowUserListDao.insert(flow1.getId(), user1, user2);
        flowUserListDao.insert(flow2.getId(), user1);
        flowUserListDao.insert(flow3.getId(), user1);

        // then:
        List<String> flowsForUser1 = flowUserListDao.findAllFlowsByUserId(user1.getUserId());
        Assert.assertEquals(3, flowsForUser1.size());

        List<String> flowsForUser2 = flowUserListDao.findAllFlowsByUserId(user2.getUserId());
        Assert.assertEquals(1, flowsForUser2.size());
    }

    private Flow createFlow(String name) {
        Flow flow = new Flow();
        flow.setName(name);
        flowDao.insert(flow);
        flowUserListDao.create(flow.getId());
        return flow;
    }
}
