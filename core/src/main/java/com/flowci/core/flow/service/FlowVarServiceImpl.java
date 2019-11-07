/*
 * Copyright 2019 flow.ci
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
import com.flowci.core.flow.domain.Flow;
import com.flowci.domain.VarType;
import com.flowci.domain.VarValue;
import com.flowci.exception.ArgumentException;
import com.flowci.util.StringHelper;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class FlowVarServiceImpl implements FlowVarService {

    @Autowired
    private FlowDao flowDao;

    @Override
    public void add(Flow flow, Map<String, VarValue> vars) {
        for (Map.Entry<String, VarValue> entry : vars.entrySet()) {
            String name = entry.getKey();
            VarValue value = entry.getValue();

            if (!StringHelper.hasValue(name)) {
                throw new ArgumentException("Var name cannot be empty");
            }

            if (!StringHelper.hasValue(value.getData())) {
                throw new ArgumentException("Var value of {0} cannot be empty", name);
            }

            boolean isVerified = VarType.verify(value.getType(), value.getData());

            if (isVerified) {
                flow.getLocally().put(name, value);
                continue;
            }

            throw new ArgumentException("Var {0} format is wrong", name);
        }

        flowDao.save(flow);
    }

    @Override
    public void remove(Flow flow, List<String> vars) {
        for (String key : vars) {
            flow.getLocally().remove(key);
        }

        flowDao.save(flow);
    }
}
