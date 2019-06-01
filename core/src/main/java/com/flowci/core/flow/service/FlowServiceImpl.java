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

import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.user.CurrentUserHelper;
import com.flowci.exception.AccessException;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
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
    private CurrentUserHelper currentUserHelper;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private CronService cronService;

    @Override
    public List<Flow> list() {
        return flowDao.findAllByCreatedBy(currentUserHelper.get().getId());
    }

    @Override
    public Flow create(String name) {
        Flow existed = flowDao.findByName(name);

        if (!Objects.isNull(existed)) {
            throw new DuplicateException("The flow {0} already existed", name);
        }

        if (!NodePath.validate(name)) {
            String message = "Illegal flow name {0}, the length cannot over 100 and '*' ',' is not available";
            throw new ArgumentException(message, name);
        }

        Flow newFlow = new Flow(name);
        newFlow.setCreatedBy(currentUserHelper.get().getId());
        return flowDao.save(newFlow);
    }

    @Override
    public Flow get(String name) {
        Flow flow = flowDao.findByNameAndCreatedBy(name, currentUserHelper.get().getId());
        if (Objects.isNull(flow)) {
            throw new NotFoundException("The flow with name {0} cannot found", name);
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
