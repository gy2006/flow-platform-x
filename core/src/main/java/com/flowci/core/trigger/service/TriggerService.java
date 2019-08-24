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

package com.flowci.core.trigger.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.service.JobService;
import com.flowci.core.trigger.domain.GitPushTrigger;
import com.flowci.core.trigger.domain.GitTrigger;
import com.flowci.core.trigger.domain.GitTrigger.GitEvent;
import com.flowci.domain.VariableMap;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.Filter;
import com.flowci.tree.Node;
import com.flowci.tree.YmlParser;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class TriggerService implements GitTriggerService {

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    protected JobService jobService;

    @Autowired
    protected ObjectMapper objectMapper;

    @Override
    public boolean startJob(String flowName, GitTrigger trigger) {
        Flow flow = get(flowName);
        Yml yml = getYml(flow);
        Node root = YmlParser.load(flow.getName(), yml.getRaw());

        if (!canStartJob(root, trigger)) {
            log.debug("Cannot start job since filter not matched on flow {}", flow.getName());
            return false;
        }

        VariableMap gitInput = trigger.toVariableMap();
        Job job = jobService.create(flow, yml, getJobTrigger(trigger), gitInput);
        jobService.start(job);
        log.debug("Start job {} from git event {} from {}", job.getId(), trigger.getEvent(), trigger.getSource());

        return true;
    }

    private Flow get(String name) {
        Flow flow = flowDao.findByName(name);
        if (Objects.isNull(flow)) {
            throw new NotFoundException("The flow with name {0} cannot found", name);
        }
        return flow;
    }

    private Yml getYml(Flow flow) {
        Optional<Yml> optional = ymlDao.findById(flow.getId());
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("No yml defined for flow {0}", flow.getName());
    }

    /**
     * Convert git trigger to job trigger
     */
    private Trigger getJobTrigger(GitTrigger trigger) {
        if (trigger.getEvent() == GitEvent.PUSH) {
            return Trigger.PUSH;
        }

        if (trigger.getEvent() == GitEvent.TAG) {
            return Trigger.TAG;
        }

        if (trigger.getEvent() == GitEvent.PR_OPEN) {
            return Trigger.PR_OPEN;
        }

        if (trigger.getEvent() == GitEvent.PR_CLOSE) {
            return Trigger.PR_CLOSE;
        }

        throw new NotFoundException("Cannot found related job trigger for {0}", trigger.getEvent().name());
    }

    private boolean canStartJob(Node root, GitTrigger trigger) {
        Filter condition = root.getFilter();

        if (trigger.getEvent() == GitEvent.PUSH) {
            GitPushTrigger pushTrigger = (GitPushTrigger) trigger;
            return condition.isMatchBranch(pushTrigger.getRef());
        }

        if (trigger.getEvent() == GitEvent.TAG) {
            GitPushTrigger tagTrigger = (GitPushTrigger) trigger;
            return condition.isMatchTag(tagTrigger.getRef());
        }

        return true;
    }
}
