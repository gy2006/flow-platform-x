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

import com.flowci.agent.dao.ExecutedCmdDao;
import com.flowci.agent.domain.AgentExecutedCmd;
import com.flowci.agent.test.SpringScenario;
import com.flowci.domain.ExecutedCmd.Status;
import java.util.Date;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class ExecutedCmdDaoTest extends SpringScenario {

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Test
    public void should_save_and_load_executed_cmd() {
        AgentExecutedCmd cmd = new AgentExecutedCmd();
        cmd.setId(UUID.randomUUID().toString());
        cmd.setStatus(Status.RUNNING);
        cmd.getOutput().putString("FLOW_AGENT", "agent");
        cmd.setCode(1);
        cmd.setError("Exception");
        cmd.setProcessId(1);
        cmd.setStartAt(new Date());
        cmd.setFinishAt(new Date());

        AgentExecutedCmd saved = executedCmdDao.save(cmd);

        Assert.assertNotNull(saved);
        Assert.assertEquals(cmd.getId(), saved.getId());
        Assert.assertEquals(Status.RUNNING, saved.getStatus());
        Assert.assertEquals("agent", saved.getOutput().getString("FLOW_AGENT"));
        Assert.assertEquals("Exception", saved.getError());
        Assert.assertNotNull(cmd.getCode());
        Assert.assertNotNull(saved.getStartAt());
        Assert.assertNotNull(saved.getFinishAt());
    }

}
