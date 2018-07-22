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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.domain.RequestMessage;
import com.flowci.core.domain.ResponseMessage;
import com.flowci.core.domain.StatusCode;
import com.flowci.core.flow.domain.Flow;
import com.flowci.util.StringHelper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * @author yang
 */
public class FlowControllerTest extends SpringTest {

    private final TypeReference<ResponseMessage<Flow>> FlowType = new TypeReference<ResponseMessage<Flow>>() {
    };

    @Autowired
    private MvcMockHelper mvcMockHelper;

    @Autowired
    private ObjectMapper objectMapper;

    private final String flowName = "hello_world";

    @Before
    public void login() {
        mockLogin();
    }

    @Before
    public void createFlowWithYml() throws Exception {
        String yml = StringHelper.toString(load("flow.yml"));
        String jsonContent = objectMapper.writeValueAsString(new RequestMessage<>(yml));

        ResponseMessage<Flow> response = mvcMockHelper
            .expectSuccessAndReturnClass(post("/flows/" + flowName)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent), FlowType);

        Assert.assertEquals(StatusCode.OK, response.getCode());
    }

    @Test
    public void should_get_or_list_flow() throws Exception {
        ResponseMessage<Flow> getFlowResponse = mvcMockHelper
            .expectSuccessAndReturnClass(get("/flows/" + flowName), FlowType);

        Assert.assertEquals(StatusCode.OK, getFlowResponse.getCode());
        Assert.assertEquals(flowName, getFlowResponse.getData().getName());
    }

}
