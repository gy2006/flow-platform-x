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

package com.flowci.core.test.trigger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.test.MvcMockHelper;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.test.flow.FlowMockHelper;
import com.flowci.domain.ObjectWrapper;
import com.flowci.util.StringHelper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;

/**
 * @author yang
 */
public class WebhookControllerTest extends SpringScenario {

    @Autowired
    private FlowMockHelper flowMockHelper;

    @Autowired
    private MvcMockHelper mvcMockHelper;

    @Before
    public void createFlow() throws Exception {
        mockLogin();
        String yml = StringHelper.toString(load("flow.yml"));
        flowMockHelper.crate("github-test", yml);
    }

    @Test
    public void should_start_job_from_github_push_event() throws Exception {
        String payload = StringHelper.toString(load("github/webhook_push.json"));

        CountDownLatch waitForJobCreated = new CountDownLatch(1);
        ObjectWrapper<Job> jobCreated = new ObjectWrapper<>();
        applicationEventMulticaster.addApplicationListener((ApplicationListener<JobCreatedEvent>) event -> {
            jobCreated.setValue(event.getJob());
            waitForJobCreated.countDown();
        });

        mvcMockHelper.expectSuccessAndReturnString(
            post("/webhooks/github-test")
                .header("X-GitHub-Event", "push")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));

        Assert.assertTrue(waitForJobCreated.await(10, TimeUnit.SECONDS));
        Assert.assertNotNull(jobCreated.getValue());
    }
}
