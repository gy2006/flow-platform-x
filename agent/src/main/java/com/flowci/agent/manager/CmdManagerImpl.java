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

package com.flowci.agent.manager;

import com.flowci.agent.dao.AgentCmdDao;
import com.flowci.agent.domain.AgentCmd;
import com.flowci.agent.event.CmdReceivedEvent;
import com.flowci.domain.Cmd;
import com.flowci.exception.NotFoundException;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component
public class CmdManagerImpl implements CmdManager {

    @Autowired
    private AgentCmdDao agentCmdDao;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public Cmd get(String id) {
        Optional<AgentCmd> optional = agentCmdDao.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("Cmd {0} is not found", id);
    }

    @Override
    public void onCmdReceived(Cmd received) {
        log.debug("Cmd received: {}", received);

        AgentCmd cmd = new AgentCmd();
        BeanUtils.copyProperties(received, cmd);
        agentCmdDao.save(cmd);

        applicationEventPublisher.publishEvent(new CmdReceivedEvent(this, received));
    }
}
