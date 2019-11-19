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

import com.flowci.core.api.domain.AddStatsItem;
import com.flowci.core.api.domain.CreateJobSummary;
import com.flowci.core.api.service.OpenRestService;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.RSACredential;
import com.flowci.core.flow.domain.StatsCounter;
import com.flowci.core.user.domain.User;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides API which calling from agent plugin
 */
@RestController
@RequestMapping("/api")
public class OpenRestController {

    @Autowired
    private OpenRestService openRestService;

    @GetMapping("/credential/{name}")
    public Credential getCredential(@PathVariable String name) {
        Credential credential = openRestService.getCredential(name);
        credential.cleanDBInfo();

        if (credential instanceof RSACredential) {
            RSACredential rsa = (RSACredential) credential;
            rsa.setPublicKey(null);
        }

        return credential;
    }

    @GetMapping("/flow/{name}/users")
    public List<User> listFlowUserEmail(@PathVariable String name) {
        return openRestService.users(name);
    }

    @PostMapping("/flow/{name}/stats")
    public void addStatsItem(@PathVariable String name,
                             @Validated @RequestBody AddStatsItem body) {
        openRestService.saveStatsForFlow(name, body.getType(), StatsCounter.from(body.getData()));
    }

    @PostMapping("/flow/{name}/job/{number}/summary")
    public void createJobSummary(@PathVariable String name,
                                 @PathVariable long number,
                                 @Validated @RequestBody CreateJobSummary body) {
        openRestService.saveJobSummary(name, number, body);
    }
}
