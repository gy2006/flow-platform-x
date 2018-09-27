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
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.domain.ExecutedCmd;
import com.flowci.tree.Node;
import com.flowci.tree.NodeTree;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class StepServiceImpl implements StepService {

    @Autowired
    private Cache jobStepCache;

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private CmdManager cmdManager;

    @Override
    public List<ExecutedCmd> init(Job job) {
        NodeTree tree = ymlManager.getTree(job);
        List<ExecutedCmd> steps = new LinkedList<>();

        for (Node node : tree.getOrdered()) {
            CmdId id = cmdManager.createId(job, node);
            steps.add(new ExecutedCmd(id.toString()));
        }

        return executedCmdDao.insert(steps);
    }

    @Override
    public List<ExecutedCmd> list(Job job) {
        return jobStepCache.get(job.getId(), () -> {
            NodeTree tree = ymlManager.getTree(job);
            List<Node> nodes = tree.getOrdered();

            List<String> cmdIdsInStr = new ArrayList<>(nodes.size());
            for (Node node : nodes) {
                CmdId cmdId = cmdManager.createId(job, node);
                cmdIdsInStr.add(cmdId.toString());
            }

            return Lists.newArrayList(executedCmdDao.findAllById(cmdIdsInStr));
        });
    }

    @Override
    public void update(ExecutedCmd cmd) {
        executedCmdDao.save(cmd);
    }
}
