/*
 * Copyright 2019 flow.ci
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

package com.flowci.core.test.api;

import com.flowci.core.api.domain.Step;
import com.flowci.core.api.service.OpenRestService;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.job.dao.ExecutedCmdDao;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.user.domain.User;
import com.flowci.domain.CmdId;
import com.flowci.domain.ExecutedCmd;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

public class OpenRestServiceTest extends SpringScenario {

    @Autowired
    private FlowService flowService;

    @Autowired
    private OpenRestService openRestService;

    @MockBean
    private ExecutedCmdDao executedCmdDao;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_list_all_flow_users() {
        Flow flow = flowService.create("user-test");

        List<User> users = openRestService.users(flow.getName());
        Assert.assertEquals(1, users.size());

        User user = users.get(0);
        Assert.assertNotNull(user.getEmail());

        Assert.assertNull(user.getPasswordOnMd5());
        Assert.assertNull(user.getRole());
        Assert.assertNull(user.getId());
        Assert.assertNull(user.getCreatedAt());
        Assert.assertNull(user.getCreatedBy());
        Assert.assertNull(user.getUpdatedAt());
        Assert.assertNull(user.getUpdatedBy());
    }

    @Test
    public void should_list_all_steps() {
        // given:
        Flow flow = flowService.create("user-test");
        long buildNumber = 2L;

        List<ExecutedCmd> list = Lists.newArrayList(
                createDummyCmd(flow, buildNumber, "step 1"),
                createDummyCmd(flow, buildNumber, "step 2"),
                createDummyCmd(flow, buildNumber, "step 3")
        );

        Mockito.when(executedCmdDao.findByFlowIdAndBuildNumber(flow.getId(), buildNumber)).thenReturn(list);

        // when:
        List<Step> steps = openRestService.steps(flow.getName(), buildNumber);
        Assert.assertNotNull(steps);

        // then:
        Assert.assertEquals(3, steps.size());
        Assert.assertEquals("step 1", steps.get(0).getName());
        Assert.assertEquals("step 2", steps.get(1).getName());
        Assert.assertEquals("step 3", steps.get(2).getName());
    }

    private ExecutedCmd createDummyCmd(Flow flow, long num, String name) {
        CmdId id = new CmdId();
        id.setJobId("xx");
        id.setNodePath("hello/" + name);

        ExecutedCmd cmd = new ExecutedCmd();
        cmd.setFlowId(flow.getId());
        cmd.setBuildNumber(num);
        cmd.setCmdId(id);
        cmd.setStatus(ExecutedCmd.Status.SUCCESS);
        return cmd;
    }
}
