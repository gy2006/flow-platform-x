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

package com.flowci.core.flow.service;

import com.flowci.core.config.ConfigProperties;
import com.flowci.core.domain.Variables;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.user.CurrentUserHelper;
import com.flowci.domain.VariableMap;
import com.flowci.exception.AccessException;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.NodePath;
import com.flowci.tree.YmlParser;
import com.google.common.base.Strings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class FlowServiceImpl implements FlowService {

    @Autowired
    private ConfigProperties appProperties;

    @Autowired
    private CurrentUserHelper currentUserHelper;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private CronService cronService;

    @Override
    public List<Flow> list(Status status) {
        String userId = currentUserHelper.get().getId();
        return flowDao.findAllByStatusAndCreatedBy(status, userId);
    }

    @Override
    public Flow create(String name) {
        if (!NodePath.validate(name)) {
            String message = "Illegal flow name {0}, the length cannot over 100 and '*' ',' is not available";
            throw new ArgumentException(message, name);
        }

        Flow flow = flowDao.findByName(name);

        // create a new one
        if (Objects.isNull(flow)) {
            Flow newFlow = new Flow(name);
            newFlow.setCreatedBy(currentUserHelper.get().getId());

            VariableMap vars = newFlow.getVariables();
            vars.put(Variables.App.Url, appProperties.getServerAddress());
            vars.put(Variables.Flow.Name, name);
            vars.put(Variables.Flow.Webhook, getWebhook(name));

            return flowDao.save(newFlow);
        }

        // return pending
        if (flow.getStatus() == Status.PENDING) {
            return flow;
        }

        throw new DuplicateException("Flow {0} already exists", name);
    }

    @Override
    public Flow confirm(String name) {
        Flow flow = get(name);

        if (flow.getStatus() == Status.CONFIRMED) {
            throw new NotAvailableException("Flow {0} is created", name);
        }

        flow.setStatus(Status.CONFIRMED);
        flowDao.save(flow);
        return flow;
    }

    @Override
    public Flow get(String name) {
        Flow flow = flowDao.findByNameAndCreatedBy(name, currentUserHelper.get().getId());
        if (Objects.isNull(flow)) {
            throw new NotFoundException("Flow {0} is not found", name);
        }
        return flow;
    }

    @Override
    public Flow getById(String id) {
        Optional<Flow> optional = flowDao.findById(id);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Invalid flow id {0}", id);
    }

    @Override
    public Flow delete(String name) {
        Flow flow = get(name);
        flowDao.delete(flow);

        try {
            Yml yml = getYml(flow);
            ymlDao.delete(yml);
        } catch (NotFoundException ignore) {

        }

        return flow;
    }

    @Override
    public void update(Flow flow) {
        verifyFlowIdAndUser(flow);
        flowDao.save(flow);
    }

    @Override
    public Yml getYml(Flow flow) {
        verifyFlowIdAndUser(flow);
        Optional<Yml> optional = ymlDao.findById(flow.getId());
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("No yml defined for flow {0}", flow.getName());
    }

    @Override
    public Yml saveYml(Flow flow, String yml) {
        verifyFlowIdAndUser(flow);

        if (Strings.isNullOrEmpty(yml)) {
            throw new ArgumentException("Yml content cannot be null or empty");
        }

        YmlParser.load(flow.getName(), yml);
        Yml ymlObj = new Yml(flow.getId(), yml);
        ymlObj.setCreatedBy(currentUserHelper.get().getId());
        ymlDao.save(ymlObj);

        // update cron task
        cronService.update(flow, ymlObj);
        return ymlObj;
    }

    private String getWebhook(String name) {
        return appProperties.getServerAddress() + "/webhooks/" + name;
    }

    private void verifyFlowIdAndUser(Flow flow) {
        String flowId = flow.getId();
        if (Strings.isNullOrEmpty(flowId)) {
            throw new ArgumentException("The flow id is missing");
        }

        if (!Objects.equals(flow.getCreatedBy(), currentUserHelper.get().getId())) {
            throw new AccessException("Illegal account for flow {0}", flow.getName());
        }
    }
}
