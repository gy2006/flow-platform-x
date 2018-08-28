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
import com.flowci.core.job.JobService;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.domain.ObjectWrapper;
import com.flowci.util.StringHelper;
import java.io.IOException;
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
@FixMethodOrder(MethodSorters.JVM)
public class JobServiceTest extends SpringScenario {

    @Autowired
    private FlowService flowService;

    @Autowired
    private JobService jobService;

    private Flow flow;

    private Yml yml;

    @Before
    public void mockFlowAndYml() throws IOException {
        flow = flowService.create("hello");
        yml = flowService.saveYml(flow, StringHelper.toString(load("flow.yml")));
    }

    @Before
    public void userLogin() {
        mockLogin();
    }

    @Test
    public void should_start_new_job() throws Throwable {
        ObjectWrapper<Job> receivedJob = new ObjectWrapper<>();

        // init: register JobReceivedEvent
        CountDownLatch waitForJobFromQueue = new CountDownLatch(1);
        applicationEventMulticaster.addApplicationListener((ApplicationListener<JobReceivedEvent>) event -> {
            receivedJob.setValue(event.getJob());
            waitForJobFromQueue.countDown();
        });

        // when:
        Job job = jobService.start(flow, yml, Trigger.MANUAL);
        Assert.assertNotNull(job);
        Assert.assertNotNull(jobService.getTree(job));

        // then: confirm job is received from queue
        waitForJobFromQueue.await(10, TimeUnit.SECONDS);
        Assert.assertEquals(0, waitForJobFromQueue.getCount());
        Assert.assertEquals(job, receivedJob.getValue());
    }

    @Test
    public void should_get_job_expire() {
        Job job = jobService.start(flow, yml, Trigger.MANUAL);
        Assert.assertFalse(jobService.isExpired(job));
    }
}
