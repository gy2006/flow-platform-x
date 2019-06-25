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

package com.flowci.core.test.flow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.credential.domain.RSAKeyPair;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.event.GitTestEvent;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.VariableMap;
import com.flowci.domain.http.ResponseMessage;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.YmlException;
import com.flowci.util.StringHelper;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;

/**
 * @author yang
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class FlowServiceTest extends SpringScenario {

    @Autowired
    private FlowService flowService;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_create_flow_by_name() {
        String name = "hello";
        flowService.create(name);
        Assert.assertNotNull(flowService.get(name));
        flowService.confirm(name);

        List<Flow> flows = flowService.list(Status.CONFIRMED);
        Assert.assertEquals(1, flows.size());
    }

    @Test
    public void should_update_flow_variables() {
        Flow flow = flowService.create("hello");
        flow.getVariables().put("FLOW_NAME", "hello.world");
        flowService.update(flow);

        VariableMap variables = flowService.get(flow.getName()).getVariables();
        Assert.assertEquals("hello.world", variables.get("FLOW_NAME"));
    }

    @Test
    public void should_save_yml_for_flow() throws IOException {
        // when:
        Flow flow = flowService.create("hello");
        String ymlRaw = StringHelper.toString(load("flow.yml"));

        // then: yml object should be created
        Yml yml = flowService.saveYml(flow, ymlRaw);
        Assert.assertNotNull(yml);
        Assert.assertEquals(flow.getId(), yml.getId());
    }

    @Test(expected = ArgumentException.class)
    public void should_throw_exception_if_flow_name_is_invalid_when_create() {
        String name = "hello.world";
        flowService.create(name);
    }

    @Test(expected = YmlException.class)
    public void should_throw_exception_if_yml_illegal_yml_format() {
        Flow flow = flowService.create("test");
        flowService.saveYml(flow, "hello-...");
    }

    @Test
    public void should_test_git_connection_by_list_remote_branches() throws IOException, InterruptedException {
        // init: load private key
        TypeReference<ResponseMessage<RSAKeyPair>> keyPairResponseType =
            new TypeReference<ResponseMessage<RSAKeyPair>>() {
            };

        ResponseMessage<RSAKeyPair> r = objectMapper.readValue(load("rsa-test.json"), keyPairResponseType);

        // given:
        Flow flow = flowService.create("git-test");
        CountDownLatch countDown = new CountDownLatch(2);
        List<String> branches = new LinkedList<>();

        applicationEventMulticaster.addApplicationListener((ApplicationListener<GitTestEvent>) event -> {
            if (!event.getFlowId().equals(flow.getId())) {
                return;
            }

            if (event.getStatus() == GitTestEvent.Status.FETCHING) {
                countDown.countDown();
            }

            if (event.getStatus() == GitTestEvent.Status.DONE) {
                countDown.countDown();
                branches.addAll(event.getBranches());
            }
        });

        // when:
        String gitUrl = "git@github.com:FlowCI/docs.git";
        String privateKey = r.getData().getPrivateKey();
        flowService.testGitConnection(flow.getName(), gitUrl, privateKey);

        // then:
        countDown.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(branches.size() >= 1);
    }

    @Test
    public void should_create_default_template_yml() {
        Flow flow = new Flow("hello");
        flow.getVariables().put("FLOWCI_FLOW_NAME", "hello");
        flow.getVariables().put("FLOWCI_GIT_URL", "git@github.com:FlowCI/docs.git");

        String templateYml = flowService.getTemplateYml(flow);
        Assert.assertNotNull(templateYml);
    }
}
