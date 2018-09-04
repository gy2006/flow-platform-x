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
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.config.ConfigProperties;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.helper.ThreadHelper;
import com.flowci.core.job.dao.ExecutedCmdDao;
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
import com.flowci.core.job.util.CmdHelper;
import com.flowci.core.job.util.JobKeyBuilder;
import com.flowci.core.job.util.StatusHelper;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.domain.Cmd;
import com.flowci.domain.ExecutedCmd;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
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
    private ExecutedCmdDao executedCmdDao;

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
        job.setKey(JobKeyBuilder.build(flow, buildNumber));
        job.setFlowId(flow.getId());
        job.setTrigger(trigger);
        job.setCreatedBy(getCurrentUser().getId());
        job.setBuildNumber(buildNumber);
        job.setCurrentPath(root.getPathAsString());

        Instant expireAt = Instant.now().plus(jobProperties.getExpireInSeconds(), ChronoUnit.SECONDS);
        job.setExpireAt(Date.from(expireAt));
        jobDao.save(job);

        // create job yml
        JobYml jobYml = new JobYml(job.getId(), flow.getName(), yml.getRaw());
        jobYmlDao.save(jobYml);

        return job;
    }

    @Override
    public Job start(Job job) {
        if (job.getStatus() != Job.Status.PENDING) {
            throw new StatusException("Job not in pending status");
        }

        try {
            return enqueue(job);
        } catch (StatusException e) {
            return setJobStatus(job, Job.Status.FAILURE, e.getMessage());
        }
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
    public boolean dispatch(Job job, Agent agent) {
        NodeTree tree = getTree(job);
        Node node = tree.get(currentNodePath(job));

        try {
            Cmd cmd = CmdHelper.create(job, node);
            agentService.dispatch(cmd, agent);

            if (job.getStatus() != Job.Status.RUNNING) {
                setJobStatus(job, Job.Status.RUNNING, null);
            }

            return true;
        } catch (Throwable e) {
            setJobStatus(job, Job.Status.FAILURE, e.getMessage());
            return false;
        }
    }

    @Override
    @RabbitListener(queues = "${app.job.queue-name}")
    public void processJob(Job job) {
        applicationEventPublisher.publishEvent(new JobReceivedEvent(this, job));

        if (!job.isPending()) {
            log.info("Job {} cannot be process since status not pending", job.getId());
            return;
        }

        try {
            // find available agent and tryLock
            Agent available = agentService.find(Status.IDLE, null);
            Boolean isLocked = agentService.tryLock(available);

            // re-enqueue to job while agent been locked by other
            if (!isLocked) {
                retry(job);
                return;
            }

            NodeTree tree = getTree(job);
            Node next = tree.next(currentNodePath(job));

            if (Objects.isNull(next)) {
                log.debug("Next node cannot be found when process job {}", job);
                return;
            }

            // set path and agent id to job
            job.setCurrentPath(next.getPathAsString());
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
    @RabbitListener(queues = "${app.job.callback-queue-name}")
    public void processCallback(ExecutedCmd execCmd) {
        CmdHelper.CmdID cmdId = CmdHelper.parseID(execCmd.getId());

        // get cmd related job
        Job job = jobDao.findById(cmdId.getJobId()).get();
        NodePath currentFromCmd = NodePath.create(cmdId.getNodePath());

        // verify job node path is match cmd node path
        if (!currentFromCmd.equals(currentNodePath(job))) {
            log.error("Invalid executed cmd callback: does not match job current node path");
            return;
        }

        // verify job status
        if (!job.isRunning()) {
            log.error("Cannot handle cmd callback since job is not running: {}", job.getStatus());
            return;
        }

        // save executed cmd
        executedCmdDao.save(execCmd);
        log.debug("Executed cmd {} been recorded", execCmd);

        // merge job context
        job.getContext().merge(execCmd.getOutput());
        jobDao.save(job);

        // continue to run next node
        if (execCmd.isSuccess()) {
            handleSuccessCmd(job);
            return;
        }

        handleFailureCmd(job, execCmd);
    }

    private void handleFailureCmd(Job job, ExecutedCmd execCmd) {
        Job.Status jobStatus = StatusHelper.convert(execCmd.getStatus());

        NodeTree tree = getTree(job);
        NodePath path = currentNodePath(job);

        Node current = tree.get(path);
        Node next = tree.next(path);

        // set job status
        // - no more node to be executed
        // - current node not allow failure
        if (Objects.isNull(next) || !current.isAllowFailure()) {
            setJobStatus(job, jobStatus, execCmd.getError());
            log.info("Job {} been executed with status {}", job.getId(), jobStatus);
            return;
        }

        setupNodePathAndDispatch(job, next);
    }

    private void handleSuccessCmd(Job job) {
        NodeTree tree = getTree(job);
        Node next = tree.next(currentNodePath(job));

        if (Objects.isNull(next)) {
            setJobStatus(job, Job.Status.SUCCESS, null);
            log.info("Job {} been executed", job.getId());
            return;
        }

        setupNodePathAndDispatch(job, next);
    }

    private void setupNodePathAndDispatch(Job job, Node next) {
        job.setCurrentPath(next.getPathAsString());
        jobDao.save(job);

        Agent agent = agentService.get(job.getAgentId());
        dispatch(job, agent);
    }

    private NodePath currentNodePath(Job job) {
        return NodePath.create(job.getCurrentPath());
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
            setJobStatus(job, Job.Status.TIMEOUT, null);
            log.warn("Job '{}' is expired", job);
            return job;
        }

        try {
            queueTemplate.convertAndSend(jobQueue.getName(), job);
            applicationEventPublisher.publishEvent(new JobCreatedEvent(this, job));
            setJobStatus(job, Job.Status.ENQUEUE, null);
            return job;
        } catch (Throwable e) {
            throw new StatusException("Unable to enqueue the job {} since {}", job.getId(), e.getMessage());
        }
    }

    private Job setJobStatus(Job job, Job.Status newStatus, String message) {
        job.setStatus(newStatus);
        job.setMessage(message);
        jobDao.save(job);
        applicationEventPublisher.publishEvent(new StatusChangeEvent(this, job));
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
