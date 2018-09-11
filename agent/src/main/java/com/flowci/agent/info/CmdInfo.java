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

package com.flowci.agent.info;

import com.flowci.agent.service.CmdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class CmdInfo implements InfoContributor {

    @Autowired
    private CmdService cmdService;

    @Override
    public void contribute(Builder builder) {
        builder.withDetail("received", cmdService.listReceivedCmd(0, 20).getContent());
        builder.withDetail("executed", cmdService.listExecutedCmd(0, 20).getContent());
    }
}
