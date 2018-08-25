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

package com.flowci.core.job;

import com.flowci.core.RequireCurrentUser;
import com.flowci.core.agent.AgentService;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobNumberDao;
import com.flowci.core.job.dao.JobYmlDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobNumber;
import com.flowci.core.job.domain.JobYml;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.exception.NotFoundException;
import java.util.Optional;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service
public class JobServiceImpl extends RequireCurrentUser implements JobService {

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobYmlDao jobYmlDao;

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private RabbitTemplate queueTemplate;

    @Autowired
    private Queue jobQueue;

    @Autowired
    private AgentService agentService;

    @Override
    public Job start(Flow flow, Yml yml, Trigger trigger) {
        // create job number
        Long buildNumber = getJobNumber(flow);

        // create job
        Job job = new Job();
        job.setKey(JobKeyBuilder.build(flow, buildNumber));
        job.setFlowId(flow.getId());
        job.setTrigger(trigger);
        job.setCreatedBy(getCurrentUser().getId());
        job.setBuildNumber(buildNumber);
        jobDao.save(job);

        // create job yml
        JobYml jobYml = new JobYml(job.getId(), yml.getRaw());
        jobYmlDao.save(jobYml);
        return enqueue(job);
    }

    @Override
    @RabbitListener(queues = "${app.job.queue-name}")
    public void processJob(Job job) {
        applicationEventPublisher.publishEvent(new JobReceivedEvent(this, job));

        // select agent
        try {
            Agent agent = agentService.find(Status.IDLE, null);
        } catch (NotFoundException e) {
            enqueue(job);
        }

        // send job to agent queue
    }

    private Job enqueue(Job job) {
        queueTemplate.convertAndSend(jobQueue.getName(), job);
        applicationEventPublisher.publishEvent(new JobCreatedEvent(this, job));
        return job;
    }

    private Long getJobNumber(Flow flow) {
        Optional<JobNumber> optional = jobNumberDao.findById(flow.getId());
        if (optional.isPresent()) {
            JobNumber number = optional.get();
            number.setNumber(number.getNumber() + 1);
            return jobNumberDao.save(number).getNumber();
        }

        return jobNumberDao.save(new JobNumber(flow.getId())).getNumber();
    }
}
