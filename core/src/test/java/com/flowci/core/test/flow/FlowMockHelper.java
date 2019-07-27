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
import com.flowci.core.common.domain.StatusCode;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.test.MvcMockHelper;
import com.flowci.domain.http.ResponseMessage;
import java.util.List;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/**
 * @author yang
 */
public class FlowMockHelper {

    public final static TypeReference<ResponseMessage<Flow>> FlowType =
        new TypeReference<ResponseMessage<Flow>>() {
        };

    public final static TypeReference<ResponseMessage<List<Flow>>> ListFlowType =
        new TypeReference<ResponseMessage<List<Flow>>>() {
        };

    @Autowired
    private MvcMockHelper mvcMockHelper;

    public Flow crate(String name, String yml) throws Exception {
        ResponseMessage<Flow> response = mvcMockHelper
            .expectSuccessAndReturnClass(post("/flows/" + name)
                .contentType(MediaType.TEXT_PLAIN)
                .content(yml), FlowType);

        Assert.assertEquals(StatusCode.OK, response.getCode());
        return response.getData();
    }
}
