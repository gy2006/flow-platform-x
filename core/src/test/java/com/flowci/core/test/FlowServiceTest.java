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

package com.flowci.core.test;

import com.flowci.core.flow.FlowService;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.domain.VariableMap;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.YmlException;
import com.flowci.util.StringHelper;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
@FixMethodOrder(value = MethodSorters.JVM)
public class FlowServiceTest extends SpringTest {

    @Autowired
    private FlowService flowService;

    @Before
    public void login() {
        mockLogin();
    }

    @Test
    public void should_create_flow_by_name() {
        flowService.create("hello");
        Assert.assertNotNull(flowService.get("hello"));
    }

    @Test
    public void should_update_flow_variables() {
        Flow flow = flowService.create("hello");
        flow.getVariables().putString("FLOW_NAME", "hello.world");
        flowService.update(flow);

        VariableMap variables = flowService.get(flow.getName()).getVariables();
        Assert.assertEquals("hello.world", variables.getString("FLOW_NAME"));
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

}
