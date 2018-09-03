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

package com.flowci.agent.event;

import com.flowci.domain.Cmd;
import com.flowci.domain.ExecutedCmd;
import lombok.Getter;

/**
 * @author yang
 */
public class CmdCompleteEvent extends CmdEvent {

    @Getter
    private final ExecutedCmd executed;

    public CmdCompleteEvent(Object source, Cmd cmd, ExecutedCmd executed) {
        super(source, cmd);
        this.executed = executed;
    }
}
