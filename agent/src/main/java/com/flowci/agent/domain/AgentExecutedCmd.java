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

package com.flowci.agent.domain;

import com.flowci.agent.domain.converter.VariableMapConverter;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.VariableMap;
import java.util.Date;
import javax.persistence.Convert;
import org.springframework.data.annotation.Id;

/**
 * @author yang
 */
public class AgentExecutedCmd extends ExecutedCmd {

    @Id
    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public Integer getProcessId() {
        return super.getProcessId();
    }

    @Override
    @Convert(converter = VariableMapConverter.class)
    public VariableMap getOutput() {
        return super.getOutput();
    }

    @Override
    public Date getStartAt() {
        return super.getStartAt();
    }

    @Override
    public Date getFinishAt() {
        return super.getFinishAt();
    }

    @Override
    public String getError() {
        return super.getError();
    }
}
