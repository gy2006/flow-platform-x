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

package com.flowci.core.flow;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.FlowGitTest;
import com.flowci.core.flow.service.FlowService;
import com.flowci.domain.http.RequestMessage;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/flows")
public class FlowController {

    @Autowired
    private FlowService flowService;

    @GetMapping
    public List<Flow> list() {
        return flowService.list(Status.CONFIRMED);
    }

    @GetMapping(value = "/{name}")
    public Flow get(@PathVariable String name) {
        return flowService.get(name);
    }

    @GetMapping(value = "/{name}/exist")
    public Boolean exist(@PathVariable String name) {
        return flowService.exist(name);
    }

    @PostMapping(value = "/{name}")
    public Flow create(@PathVariable String name, @RequestBody(required = false) String yml) {
        Flow flow = flowService.create(name);
        if (!Objects.isNull(yml)) {
            flowService.saveYml(flow, yml);
        }
        return flow;
    }

    @PostMapping(value = "/{name}/git/test")
    public void gitTest(@PathVariable String name,
                        @Validated @RequestBody FlowGitTest body) {
        flowService.testGitConnection(name, body.getGitUrl(), body.getPrivateKey());
    }

    @PostMapping(value = "/{name}/confirm")
    public Flow confirm(@PathVariable String name) {
        return flowService.confirm(name);
    }

    @GetMapping(value = "/{name}/yml", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getYml(@PathVariable String name) {
        Flow flow = flowService.get(name);
        String yml = flowService.getYml(flow).getRaw();
        return Base64.getEncoder().encodeToString(yml.getBytes());
    }

    @PostMapping("/{name}/yml")
    public void updateYml(@PathVariable String name, @RequestBody RequestMessage<String> body) {
        Flow flow = flowService.get(name);
        byte[] yml = Base64.getDecoder().decode(body.getData());
        flowService.saveYml(flow, new String(yml));
    }

    @PostMapping("/{name}/variables")
    public void updateVariables(@PathVariable String name, @RequestBody Map<String, String> variables) {
        Flow flow = flowService.get(name);
        flow.getVariables().reset(variables);
        flowService.update(flow);
    }

    @DeleteMapping("/{name}")
    public Flow delete(@PathVariable String name) {
        return flowService.delete(name);
    }

    @DeleteMapping("/{name}/variables")
    public void cleanVariables(@PathVariable String name) {
        Flow flow = flowService.get(name);
        flow.getVariables().clear();
        flowService.update(flow);
    }
}
