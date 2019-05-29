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

package com.flowci.core.job.service;

import com.flowci.core.job.dao.ExecutedCmdDao;
import com.flowci.core.job.domain.CmdId;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.StepStatusChangeEvent;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.domain.ExecutedCmd;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.Node;
import com.flowci.tree.NodeTree;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * @author yang
 */
@Log4j2
@Service
public class StepServiceImpl implements StepService {

    @Autowired
    private Cache<String, List<ExecutedCmd>> jobStepCache;

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    public List<ExecutedCmd> init(Job job) {
        NodeTree tree = ymlManager.getTree(job);
        List<ExecutedCmd> steps = new LinkedList<>();

        for (Node node : tree.getOrdered()) {
            CmdId id = cmdManager.createId(job, node);
            steps.add(new ExecutedCmd(id.toString(), node.isAllowFailure()));
        }

        return executedCmdDao.insert(steps);
    }

    @Override
    public ExecutedCmd get(Job job, Node node) {
        CmdId id = cmdManager.createId(job, node);
        return get(id.toString());
    }

    @Override
    public ExecutedCmd get(String cmdId) {
        Optional<ExecutedCmd> optional = executedCmdDao.findById(cmdId);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Executed cmd {0} not found", cmdId);
    }

    @Override
    public List<ExecutedCmd> list(Job job) {
        return jobStepCache.get(job.getId(), s -> {
            NodeTree tree = ymlManager.getTree(job);
            List<Node> nodes = tree.getOrdered();

            List<ExecutedCmd> cmds = new ArrayList<>(nodes.size());
            for (Node node : nodes) {
                CmdId cmdId = cmdManager.createId(job, node);

                Optional<ExecutedCmd> optional = executedCmdDao.findById(cmdId.toString());
                optional.ifPresent(cmds::add);
            }

            return cmds;
        });
    }

    @Override
    public void update(Job job, ExecutedCmd cmd) {
        executedCmdDao.save(cmd);
        jobStepCache.invalidate(job.getId());
        applicationEventPublisher.publishEvent(new StepStatusChangeEvent(this, job, cmd));
    }
}
