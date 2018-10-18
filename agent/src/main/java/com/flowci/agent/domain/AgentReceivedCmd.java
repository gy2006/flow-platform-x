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

import com.flowci.agent.domain.converter.StringListConverter;
import com.flowci.agent.domain.converter.StringSetConverter;
import com.flowci.agent.domain.converter.VariableMapConverter;
import com.flowci.domain.Cmd;
import com.flowci.domain.CmdType;
import com.flowci.domain.VariableMap;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;

/**
 * @author yang
 */
@Data
@Entity(name = "cmd_received")
public class AgentReceivedCmd extends Cmd {

    @Column(name = "received_at")
    private Date receivedAt;

    @Id
    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    @Column(name = "allow_failure")
    public Boolean getAllowFailure() {
        return super.getAllowFailure();
    }

    @Override
    public String getPlugin() {
        return super.getPlugin();
    }

    @Override
    @Column(name = "cmd_type")
    public CmdType getType() {
        return super.getType();
    }

    @Override
    @Convert(converter = VariableMapConverter.class)
    public VariableMap getInputs() {
        return super.getInputs();
    }

    @Override
    @Convert(converter = StringListConverter.class)
    public List<String> getScripts() {
        return super.getScripts();
    }

    @Override
    @Column(name = "env_filters")
    @Convert(converter = StringSetConverter.class)
    public Set<String> getEnvFilters() {
        return super.getEnvFilters();
    }

    @Override
    @Column(name = "work_dir")
    public String getWorkDir() {
        return super.getWorkDir();
    }

    @Override
    public Long getTimeout() {
        return super.getTimeout();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
