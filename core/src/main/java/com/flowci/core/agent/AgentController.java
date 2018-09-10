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

import com.flowci.core.agent.domain.CreateAgent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.domain.Agent;
import com.flowci.domain.Settings;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/agents")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @GetMapping("/{token}")
    public Agent getByToken(@PathVariable String token) {
        return agentService.getByToken(token);
    }

    @GetMapping
    public List<Agent> list() {
        return agentService.list();
    }

    @GetMapping("/connect")
    public Settings connect(@RequestParam String token) {
        return agentService.connect(token);
    }

    @PostMapping()
    public Agent create(@RequestBody CreateAgent body) {
        return agentService.create(body.getName(), body.getTags());
    }

    @DeleteMapping("/{token}")
    public Agent delete(@PathVariable String token) {
        return agentService.delete(token);
    }

    @PatchMapping("/{token}/tags")
    public Agent setTags(@PathVariable String token, @RequestBody Set<String> tags) {
        return agentService.setTags(token, tags);
    }
}
