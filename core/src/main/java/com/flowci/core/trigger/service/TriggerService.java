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

import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.Optional;

public abstract class TriggerService implements GitTriggerService {

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

    public Flow get(String name) {
        Flow flow = flowDao.findByName(name);
        if (Objects.isNull(flow)) {
            throw new NotFoundException("The flow with name {0} cannot found", name);
        }
        return flow;
    }

    public Yml getYml(Flow flow) {
        Optional<Yml> optional = ymlDao.findById(flow.getId());
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("No yml defined for flow {0}", flow.getName());
    }
}
