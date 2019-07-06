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

import com.flowci.core.credential.domain.RSAKeyPair;
import com.flowci.core.credential.service.CredentialService;
import com.flowci.core.domain.Variables;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.FlowGitTest;
import com.flowci.core.flow.service.FlowService;
import com.flowci.domain.http.RequestMessage;
import com.flowci.exception.ArgumentException;
import com.google.common.base.Strings;
import java.util.Base64;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private CredentialService credentialService;

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
    public Flow create(@PathVariable String name) {
        return flowService.create(name);
    }

    @PostMapping(value = "/{name}/git/test")
    public void gitTest(@PathVariable String name, @Validated @RequestBody FlowGitTest body) {
        String privateKey = body.getPrivateKey();

        if (body.hasCredentialName()) {
            RSAKeyPair credential = (RSAKeyPair) credentialService.get(body.getCredential());
            privateKey = credential.getPrivateKey();
        }

        if (Strings.isNullOrEmpty(privateKey)) {
            throw new ArgumentException("Credential name or private key must be provided");
        }

        flowService.testGitConnection(name, body.getGitUrl(), privateKey);
    }

    @PostMapping("/{name}/variables")
    public void addVariables(@PathVariable String name, @RequestBody Map<String, String> variables) {
        Flow flow = flowService.get(name);
        flow.getVariables().putAll(variables);
        flowService.update(flow);
    }

    /**
     * Create credential for flow only
     * It will create default credential name: 'flow-{flow name}-ssh-rsa'
     */
    @PostMapping("/{name}/credential/rsa")
    public void setupRSACredential(@PathVariable String name, @RequestBody RSAKeyPair keyPair) {
        Flow flow = flowService.get(name);

        String credentialName = "flow-" + flow.getName() + "-ssh-rsa";
        credentialService.createRSA(credentialName, keyPair.getPublicKey(), keyPair.getPrivateKey());

        flow.getVariables().put(Variables.Credential.SSH_RSA, credentialName);
        flowService.update(flow);
    }

    @PostMapping(value = "/{name}/confirm")
    public Flow confirm(@PathVariable String name) {
        return flowService.confirm(name);
    }

    @PostMapping("/{name}/yml")
    public void setupYml(@PathVariable String name, @RequestBody RequestMessage<String> body) {
        Flow flow = flowService.get(name);
        byte[] yml = Base64.getDecoder().decode(body.getData());
        flowService.saveYml(flow, new String(yml));
    }

    @GetMapping(value = "/{name}/yml", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getYml(@PathVariable String name) {
        Flow flow = flowService.get(name);
        String yml = flowService.getYml(flow).getRaw();
        return Base64.getEncoder().encodeToString(yml.getBytes());
    }

    @DeleteMapping("/{name}")
    public Flow delete(@PathVariable String name) {
        return flowService.delete(name);
    }
}
