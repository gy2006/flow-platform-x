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

package com.flowci.agent.test.dao;

import com.flowci.agent.test.SpringScenario;
import com.flowci.agent.dao.ReceivedCmdDao;
import com.flowci.agent.domain.AgentReceivedCmd;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class AgentCmdDaoTest extends SpringScenario {

    @Autowired
    private ReceivedCmdDao agentCmdDao;

    @Test
    public void should_save_and_load_agent_cmd() {
        AgentReceivedCmd cmd = new AgentReceivedCmd();
        cmd.setId("1-hello/world");
        cmd.getInputs().putString("hello", "world");
        cmd.setScripts(Lists.newArrayList("h2"));
        cmd.setEnvFilters(Sets.newHashSet("FLOW_"));

        AgentReceivedCmd saved = agentCmdDao.save(cmd);
        Assert.assertNotNull(saved);

        AgentReceivedCmd loaded = agentCmdDao.findById(cmd.getId()).get();
        Assert.assertNotNull(loaded);
        Assert.assertEquals(cmd, loaded);
    }
}
