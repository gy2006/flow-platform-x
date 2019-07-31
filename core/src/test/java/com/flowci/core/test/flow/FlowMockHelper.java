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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.test.MvcMockHelper;
import com.flowci.domain.http.RequestMessage;
import com.flowci.domain.http.ResponseMessage;

import java.util.Base64;
import java.util.List;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * @author yang
 */
public class FlowMockHelper {

    final static TypeReference<ResponseMessage<Flow>> FlowType =
        new TypeReference<ResponseMessage<Flow>>() {
        };

    final static TypeReference<ResponseMessage<List<Flow>>> ListFlowType =
        new TypeReference<ResponseMessage<List<Flow>>>() {
        };

    final static TypeReference<ResponseMessage<String>> FlowYmlType =
            new TypeReference<ResponseMessage<String>>() {
            };

    @Autowired
    private MvcMockHelper mvcMockHelper;

    @Autowired
    private ObjectMapper objectMapper;

    public Flow crate(String name, String yml) throws Exception {
        // create
        ResponseMessage<Flow> response = mvcMockHelper
            .expectSuccessAndReturnClass(post("/flows/" + name), FlowType);

        Assert.assertEquals(StatusCode.OK, response.getCode());

        // confirm
        response = mvcMockHelper.expectSuccessAndReturnClass(post("/flows/" + name + "/confirm"), FlowType);

        // save yml
        String base64Encoded = Base64.getEncoder().encodeToString(yml.getBytes());
        RequestMessage<String> message = new RequestMessage<>(base64Encoded);

        mvcMockHelper.expectSuccessAndReturnString(
                post("/flows/" + name + "/yml")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(message))
        );

        return response.getData();
    }
}
