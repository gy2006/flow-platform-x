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

package com.flowci.core.api;

import com.flowci.core.agent.service.AgentService;
import com.flowci.core.api.domain.AddStatsItem;
import com.flowci.core.api.domain.CreateJobSummary;
import com.flowci.core.api.service.OpenRestService;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.RSACredential;
import com.flowci.core.stats.domain.StatsCounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides API which calling from agent plugin
 */
@RestController
@RequestMapping("/api")
public class OpenRestController {

    private static final String HeaderAgentToken = "AGENT-TOKEN";

    @Autowired
    private OpenRestService openRestService;

    @Autowired
    private AgentService agentService;

    @GetMapping("/credential/{name}")
    public String getRsaPrivateKey(@RequestHeader(HeaderAgentToken) String token,
                                   @PathVariable String name) {

        agentService.getByToken(token);

        Credential credential = openRestService.getCredential(name, RSACredential.class);
        RSACredential pair = (RSACredential) credential;
        return pair.getPrivateKey();
    }

    @PostMapping("/stats/{flowName}")
    public void addStatsItem(@RequestHeader(HeaderAgentToken) String token,
                             @PathVariable String flowName,
                             @Validated @RequestBody AddStatsItem body) {
        agentService.getByToken(token);
        openRestService.addStats(flowName, body.getType(), StatsCounter.from(body.getData()));
    }

    @PostMapping("/summary/{flowName}/{buildNumber}")
    public void createJobSummary(@RequestHeader(HeaderAgentToken) String token,
                                 @PathVariable String flowName,
                                 @PathVariable long buildNumber,
                                 @Validated @RequestBody CreateJobSummary body) {
        agentService.getByToken(token);
        openRestService.createJobSummary(flowName, buildNumber, body);
    }
}
