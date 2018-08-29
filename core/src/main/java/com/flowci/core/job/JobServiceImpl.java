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
import com.flowci.core.config.ConfigProperties;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.helper.ThreadHelper;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobNumberDao;
import com.flowci.core.job.dao.JobYmlDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobNumber;
import com.flowci.core.job.domain.JobYml;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.StatusChangeEvent;
import com.flowci.core.job.util.CmdBuilder;
import com.flowci.core.job.util.KeyBuilder;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.domain.Cmd;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class JobServiceImpl extends RequireCurrentUser implements JobService {

    @Autowired
    private ConfigProperties.Job jobProperties;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobYmlDao jobYmlDao;

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private Cache jobTreeCache;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private RabbitTemplate queueTemplate;

    @Autowired
    private Queue jobQueue;

    @Autowired
    private ThreadPoolTaskExecutor retryExecutor;

    @Autowired
    private AgentService agentService;

    @Override
    public Job create(Flow flow, Yml yml, Trigger trigger) {
        // verify yml and parse to Node
        Node root = YmlParser.load(flow.getName(), yml.getRaw());

        // create job number
        Long buildNumber = getJobNumber(flow);

        // create job
        Job job = new Job();
        job.setKey(KeyBuilder.buildJobKey(flow, buildNumber));
        job.setFlowId(flow.getId());
        job.setTrigger(trigger);
        job.setCreatedBy(getCurrentUser().getId());
        job.setBuildNumber(buildNumber);
        job.setCurrentPath(root.getPath().getPathInStr());

        Instant expireAt = Instant.now().plus(jobProperties.getExpireInSeconds(), ChronoUnit.SECONDS);
        job.setExpireAt(Date.from(expireAt));
        jobDao.save(job);

        // create job yml
        JobYml jobYml = new JobYml(job.getId(), flow.getName(), yml.getRaw());
        jobYmlDao.save(jobYml);

        return job;
    }

    @Override
    public void start(Job job) {
        if (job.getStatus() != Job.Status.PENDING) {
            throw new StatusException("Job not in pending status");
        }

        enqueue(job);
    }

    @Override
    public NodeTree getTree(Job job) {
        return jobTreeCache.get(job.getId(), () -> {
            log.debug("Load node tree for job: {}", job.getId());
            JobYml yml = jobYmlDao.findById(job.getId()).get();
            Node root = YmlParser.load(yml.getName(), yml.getRaw());
            return NodeTree.create(root);
        });
    }

    @Override
    public boolean isExpired(Job job) {
        Instant expireAt = job.getExpireAt().toInstant();
        return Instant.now().compareTo(expireAt) == 1;
    }

    @Override
    @RabbitListener(queues = "${app.job.queue-name}")
    public void processJob(Job job) {
        applicationEventPublisher.publishEvent(new JobReceivedEvent(this, job));

        try {
            // find available agent and tryLock
            Agent available = agentService.find(Status.IDLE, null);
            Boolean isLocked = agentService.tryLock(available);

            // re-enqueue to job while agent been locked by other
            if (!isLocked) {
                retry(job);
                return;
            }

            // set dispatched agent id to job
            job.setAgentId(available.getId());
            jobDao.save(job);

            // dispatch job to agent queue
            dispatch(job, available);

        } catch (NotFoundException e) {
            // re-enqueue to job while agent not found
            retry(job);
        }
    }

    @Override
    public boolean dispatch(Job job, Agent agent) {
        NodeTree tree = getTree(job);
        NodePath current = NodePath.create(job.getCurrentPath());

        Node nodeToDispatch = tree.next(current);
        Cmd cmd = CmdBuilder.build(job, nodeToDispatch);
        agentService.dispatch(cmd, agent);
        return true;
    }

    /**
     * Re-enqueue job after few seconds
     */
    private void retry(Job job) {
        retryExecutor.execute(() -> {
            ThreadHelper.sleep(jobProperties.getRetryWaitingSeconds() * 1000);
            enqueue(job);
        });
    }

    private Job enqueue(Job job) {
        if (isExpired(job)) {
            setJobStatus(job, Job.Status.TIMEOUT);
            log.warn("Job '{}' is expired", job);
            return job;
        }

        queueTemplate.convertAndSend(jobQueue.getName(), job);
        applicationEventPublisher.publishEvent(new JobCreatedEvent(this, job));
        return job;
    }

    private void setJobStatus(Job job, Job.Status newStatus) {
        job.setStatus(newStatus);
        jobDao.save(job);
        applicationEventPublisher.publishEvent(new StatusChangeEvent(this, job));
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
