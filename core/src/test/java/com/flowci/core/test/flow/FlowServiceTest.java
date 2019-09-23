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
import com.flowci.core.credential.domain.RSACredential;
import com.flowci.core.credential.service.CredentialService;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.event.GitTestEvent;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.test.SpringScenario;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import com.flowci.domain.http.ResponseMessage;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.YmlException;
import com.flowci.tree.Node;
import com.flowci.tree.YmlParser;
import com.flowci.util.StringHelper;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.*;
import org.junit.runners.MethodSorters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
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

    @MockBean
    private CredentialService credentialService;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_list_all_flows_by_user_id() {
        Flow first = flowService.create("test-1");
        flowService.confirm(first.getName(), null, null);

        Flow second = flowService.create("test-2");
        flowService.confirm(second.getName(), null, null);

        List<Flow> list = flowService.list(sessionManager.getUserId(), Status.CONFIRMED);
        Assert.assertEquals(2, list.size());
        Assert.assertEquals(first, list.get(0));
        Assert.assertEquals(second, list.get(1));
    }

    @Test
    public void should_create_and_confirm_flow_with_git_template() {
        // when: create flow
        String name = "hello";
        flowService.create(name);

        // then: flow with pending status
        Flow created = flowService.get(name);
        Assert.assertNotNull(created);
        Assert.assertEquals(Status.PENDING, created.getStatus());

        // when: confirm the flow
        String gitUrl = "git@github.com:FlowCI/docs.git";
        String credential = "ssh-ras-credential";
        flowService.confirm(name, gitUrl, credential);

        // then: flow should be with confirmed status
        Flow confirmed = flowService.get(name);
        Assert.assertNotNull(confirmed);
        Assert.assertEquals(Status.CONFIRMED, confirmed.getStatus());

        // then: the default yml should be created with expected variables
        Yml yml = flowService.getYml(confirmed);
        Assert.assertNotNull(yml);

        Node root = YmlParser.load("test", yml.getRaw());
        Assert.assertEquals(gitUrl, root.getEnv(Variables.Flow.GitUrl));
        Assert.assertEquals(credential, root.getEnv(Variables.Flow.SSH_RSA));

        // then:
        List<Flow> flows = flowService.list(Status.CONFIRMED);
        Assert.assertEquals(1, flows.size());
    }

    @Test
    public void should_create_and_confirm_flow_with_default_template() {
        // when: create and confirm flow without git settings
        String name = "hello";
        flowService.create(name);
        Flow confirmed = flowService.confirm(name, null, null);

        // then:
        Yml yml = flowService.getYml(confirmed);
        Assert.assertNotNull(yml);

        Node root = YmlParser.load("test", yml.getRaw());
        Assert.assertNull(root.getEnv(Variables.Flow.GitUrl));
        Assert.assertNull(root.getEnv(Variables.Flow.SSH_RSA));
    }

    @Test
    public void should_list_flow_by_credential_name() {
        String credentialName = "flow-ssh-ras-name";

        RSACredential mocked = new RSACredential();
        mocked.setName(credentialName);

        Mockito.when(credentialService.get(credentialName)).thenReturn(mocked);

        Flow flow = flowService.create("hello");
        flow.getVariables().put("FLOW_NAME", "hello.world");
        flow.getVariables().put(Variables.Flow.SSH_RSA, credentialName);
        flowService.update(flow);
        flowService.confirm(flow.getName(), null, null);

        Vars<String> variables = flowService.get(flow.getName()).getVariables();
        Assert.assertEquals(credentialName, variables.get(Variables.Flow.SSH_RSA));

        // when:
        List<Flow> flows = flowService.listByCredential(credentialName);
        Assert.assertNotNull(flows);
        Assert.assertEquals(1, flows.size());

        // then:
        Assert.assertEquals(flow.getName(), flows.get(0).getName());
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

    @Ignore
    @Test
    public void should_test_git_connection_by_list_remote_branches() throws IOException, InterruptedException {
        // init: load private key
        TypeReference<ResponseMessage<SimpleKeyPair>> keyPairResponseType =
            new TypeReference<ResponseMessage<SimpleKeyPair>>() {
            };

        ResponseMessage<SimpleKeyPair> r = objectMapper.readValue(load("rsa-test.json"), keyPairResponseType);

        // given:
        Flow flow = flowService.create("git-test");
        CountDownLatch countDown = new CountDownLatch(2);
        List<String> branches = new LinkedList<>();

        addEventListener((ApplicationListener<GitTestEvent>) event -> {
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
        countDown.await(60, TimeUnit.SECONDS);
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
