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

import com.flowci.core.agent.service.AgentService;
import com.flowci.core.domain.JsonablePage;
import com.flowci.core.job.dao.ExecutedCmdDao;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.CmdId;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.domain.Agent;
import com.flowci.domain.ExecutedCmd;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.tree.Node;
import com.flowci.tree.NodeTree;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author yang
 */
@Log4j2
@Service
public class StepServiceImpl implements StepService {

    private static final ParameterizedTypeReference<JsonablePage<String>> AgentLogsType =
        new ParameterizedTypeReference<JsonablePage<String>>() {
        };

    @Autowired
    private Cache jobStepCache;

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private AgentService agentService;

    @Autowired
    private RestTemplate restTemplate;

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
    public List<ExecutedCmd> list(Job job) {
        return jobStepCache.get(job.getId(), () -> {
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
    public Page<String> logs(Job job, String executedCmdId, Pageable pageable) {
        CmdId cmdId = CmdId.parse(executedCmdId);

        if (!getJob(cmdId.getJobId()).equals(job)) {
            throw new StatusException("Job does not matched");
        }

        Agent agent = agentService.get(job.getAgentId());

        if (!agent.hasHost()) {
            throw new StatusException("Agent host not available");
        }

        URI agentUri = UriComponentsBuilder.fromHttpUrl(agent.getHost())
            .pathSegment("cmd", executedCmdId, "logs")
            .queryParam("page", pageable.getPageNumber())
            .queryParam("size", pageable.getPageSize())
            .build()
            .toUri();

        try {
            RequestEntity<Object> request = new RequestEntity<>(HttpMethod.GET, agentUri);
            ResponseEntity<JsonablePage<String>> entity = restTemplate.exchange(request, AgentLogsType);
            return entity.getBody().toPage();
        } catch (RestClientException e) {
            throw new StatusException("Agent not available: {0}", e.getMessage());
        }
    }

    @Override
    public void update(ExecutedCmd cmd) {
        executedCmdDao.save(cmd);
    }

    private Job getJob(String id) {
        Optional<Job> optional = jobDao.findById(id);
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("Job {0} is not existed");
    }
}
