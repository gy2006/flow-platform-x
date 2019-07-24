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

package com.flowci.core.agent;

import com.flowci.core.agent.domain.AgentInit;
import com.flowci.core.agent.domain.CreateOrUpdateAgent;
import com.flowci.core.agent.domain.DeleteAgent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.job.service.LoggingService;
import com.flowci.domain.Agent;
import com.flowci.domain.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

import com.flowci.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author yang
 */
@RestController
@RequestMapping("/agents")
public class AgentController {

    private static final String HeaderAgentToken = "AGENT-TOKEN";

    @Autowired
    private AgentService agentService;

    @Autowired
    private LoggingService loggingService;

    @GetMapping("/{name}")
    public Agent getByName(@PathVariable String name) {
        return agentService.getByName(name);
    }

    @GetMapping
    public List<Agent> list() {
        return agentService.list();
    }

    @PostMapping()
    public Agent createOrUpdate(@RequestBody CreateOrUpdateAgent body) {
        if (body.hasToken()) {
            return agentService.update(body.getToken(), body.getName(), body.getTags());
        }

        return agentService.create(body.getName(), body.getTags());
    }

    @DeleteMapping()
    public Agent delete(@RequestBody DeleteAgent body) {
        return agentService.delete(body.getToken());
    }

    // --------------------------------------------------------
    //      Functions require agent token header
    // --------------------------------------------------------

    @PostMapping("/connect")
    public Settings connect(@RequestHeader(HeaderAgentToken) String token,
                            @RequestBody AgentInit init,
                            HttpServletRequest request) {
        init.setToken(token);
        init.setIp(request.getRemoteHost());
        return agentService.connect(init);
    }

    @PostMapping("/logs/upload")
    public void upload(@RequestHeader(HeaderAgentToken) String token,
                       @RequestPart("file") MultipartFile file) {

        Agent agent = agentService.getByToken(token);
        if (Objects.isNull(agent)) {
            throw new NotFoundException("Agent not existed");
        }

        try(InputStream stream = file.getInputStream()) {
            loggingService.save(file.getOriginalFilename(), stream);
        } catch (IOException ignored) {

        }
    }
}
