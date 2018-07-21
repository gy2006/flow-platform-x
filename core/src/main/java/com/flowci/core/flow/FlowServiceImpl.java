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

package com.flowci.core.flow;

import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.domain.node.NodePath;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.google.common.base.Strings;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class FlowServiceImpl implements FlowService {

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

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

        return flowDao.save(new Flow(name));
    }

    @Override
    public Flow get(String name) {
        return flowDao.findByName(name);
    }

    @Override
    public Yml saveYml(Flow flow, String yml) {
        String flowId = flow.getId();
        if (Strings.isNullOrEmpty(flowId)) {
            throw new ArgumentException("The flow id is missing");
        }

        if (Strings.isNullOrEmpty(yml)) {
            throw new ArgumentException("Yml content cannot be null or empty");
        }

        return ymlDao.save(new Yml(flowId, yml));
    }
}
