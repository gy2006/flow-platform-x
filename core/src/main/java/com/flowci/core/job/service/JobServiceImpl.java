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

package com.flowci.core.job.service;

import static com.flowci.core.trigger.domain.GitTrigger.Variables.GIT_AUTHOR;

import com.flowci.core.agent.service.AgentService;
import com.flowci.core.config.ConfigProperties;
import com.flowci.core.domain.Variables;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.helper.ThreadHelper;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobNumberDao;
import com.flowci.core.job.domain.CmdId;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobNumber;
import com.flowci.core.job.domain.JobYml;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.util.JobKeyBuilder;
import com.flowci.core.job.util.StatusHelper;
import com.flowci.core.user.CurrentUserHelper;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.domain.Cmd;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.VariableMap;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class JobServiceImpl implements JobService {

    private static final Sort SortByBuildNumber = Sort.by(Direction.DESC, "buildNumber");

    @Autowired
    private ConfigProperties appProperties;

    @Autowired
    private ConfigProperties.Job jobProperties;

    @Autowired
    private CurrentUserHelper currentUserHelper;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private RabbitTemplate queueTemplate;

    @Autowired
    private Queue jobQueue;

    @Autowired
    private ThreadPoolTaskExecutor retryExecutor;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private AgentService agentService;

    @Autowired
    private StepService stepService;

    @Override
    public Job get(Flow flow, Long buildNumber) {
        String key = JobKeyBuilder.build(flow, buildNumber);
        Job job = jobDao.findByKey(key);

        if (Objects.isNull(job)) {
            throw new NotFoundException(
                "The job {0} for build number {1} cannot found", flow.getName(), buildNumber.toString());
        }

        return job;
    }

    @Override
    public JobYml getYml(Job job) {
        return ymlManager.get(job);
    }

    @Override
    public Job getLatest(Flow flow) {
        JobNumber latest = jobNumberDao.findById(flow.getId()).get();
        return get(flow, latest.getNumber());
    }

    @Override
    public Page<Job> list(Flow flow, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, SortByBuildNumber);
        return jobDao.findAllByFlowId(flow.getId(), pageable);
    }

    @Override
    public Job create(Flow flow, Yml yml, Trigger trigger, VariableMap input) {
        // verify yml and parse to Node
        Node root = YmlParser.load(flow.getName(), yml.getRaw());

        // create job number
        Long buildNumber = getJobNumber(flow);

        // create job
        Job job = new Job();
        job.setKey(JobKeyBuilder.build(flow, buildNumber));
        job.setFlowId(flow.getId());
        job.setTrigger(trigger);
        job.setBuildNumber(buildNumber);
        job.setCurrentPath(root.getPathAsString());
        job.setAgentSelector(root.getSelector());

        // init job context
        VariableMap defaultContext = initJobContext(flow, buildNumber, root.getEnvironments(), input);
        job.getContext().merge(defaultContext);

        // setup created by form login user or git event author
        if (currentUserHelper.hasLogin()) {
            job.setCreatedBy(currentUserHelper.get().getId());
        } else {
            String createdBy = job.getContext().get(GIT_AUTHOR, "Unknown");
            job.setCreatedBy(createdBy);
        }

        // set expire at
        Instant expireAt = Instant.now().plus(jobProperties.getExpireInSeconds(), ChronoUnit.SECONDS);
        job.setExpireAt(Date.from(expireAt));
        jobDao.save(job);

        // create job yml
        ymlManager.create(flow, job, yml);

        // init job steps as executed cmd
        stepService.init(job);

        applicationEventPublisher.publishEvent(new JobCreatedEvent(this, job));
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
    public Job cancel(Job job) {
        // send stop cmd when is running
        if (job.isRunning()) {
            Agent agent = agentService.get(job.getAgentId());
            Cmd killCmd = cmdManager.createKillCmd();

            agentService.dispatch(killCmd, agent);
            log.info("Stop cmd been send to {} for job {}", agent.getName(), job.getId());
        }

        return job;
    }

    @Override
    public boolean isExpired(Job job) {
        Instant expireAt = job.getExpireAt().toInstant();
        return Instant.now().compareTo(expireAt) == 1;
    }

    @Override
    public boolean dispatch(Job job) {
        NodeTree tree = ymlManager.getTree(job);
        Node node = tree.get(currentNodePath(job));
        Agent agent = agentService.get(job.getAgentId());

        try {
            Cmd cmd = cmdManager.createShellCmd(job, node);
            agentService.dispatch(cmd, agent);

            // set job status to running
            if (!job.isRunning()) {
                setJobStatus(job, Job.Status.RUNNING, null);
            }

            // set executed cmd step to running
            ExecutedCmd executedCmd = stepService.get(job, node);
            if (!executedCmd.isRunning()) {
                executedCmd.setStatus(ExecutedCmd.Status.RUNNING);
                stepService.update(job, executedCmd);
            }

            return true;
        } catch (Throwable e) {
            setJobStatus(job, Job.Status.FAILURE, e.getMessage());
            agentService.tryRelease(agent);
            return false;
        }
    }

    @Override
    @RabbitListener(queues = "${app.job.queue-name}", containerFactory = "jobAndCallbackContainerFactory")
    public void processJob(Job job) {
        log.debug("Job {} received from queue", job.getId());
        applicationEventPublisher.publishEvent(new JobReceivedEvent(this, job));

        if (!job.isQueuing()) {
            log.info("Job {} cannot be process since status not queuing", job.getId());
            return;
        }

        try {
            // find available agents
            Set<String> agentTags = job.getAgentSelector().getTags();
            List<Agent> availableList = agentService.find(Status.IDLE, agentTags);

            // try to lock it
            Boolean isLocked = Boolean.FALSE;
            Agent available = null;

            for (Agent agent : availableList) {
                isLocked = agentService.tryLock(agent);
                if (isLocked) {
                    available = agent;
                    break;
                }
            }

            // re-enqueue to job while agent been locked by other
            if (!isLocked) {
                retry(job);
                return;
            }

            NodeTree tree = ymlManager.getTree(job);
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
            dispatch(job);
            log.debug("Job {} been dispatched to agent {}", job.getId(), available.getName());

        } catch (NotFoundException e) {
            // re-enqueue to job while agent not found
            log.debug("Agent not available, job {} retry", job.getId());
            retry(job);
        }
    }

    @Override
    @RabbitListener(queues = "${app.job.callback-queue-name}", containerFactory = "jobAndCallbackContainerFactory")
    public void processCallback(ExecutedCmd execCmd) {
        CmdId cmdId = CmdId.parse(execCmd.getId());
        if (Objects.isNull(cmdId)) {
            log.debug("Illegal cmd callback: {}", execCmd.getId());
            return;
        }

        // get cmd related job
        Job job = jobDao.findById(cmdId.getJobId()).get();
        NodePath currentFromCmd = NodePath.create(cmdId.getNodePath());

        NodeTree tree = ymlManager.getTree(job);
        Node node = tree.get(currentFromCmd);

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
        stepService.update(job, execCmd);
        log.debug("Executed cmd {} been recorded", execCmd);

        // merge output to job context
        VariableMap context = job.getContext();
        context.merge(execCmd.getOutput());

        // setup current job status if not final node
        if (!node.isFinal()) {
            context.putString(Variables.JOB_STATUS, StatusHelper.convert(execCmd).name());
        }

        jobDao.save(job);

        // find next node
        Node next = execCmd.isSuccess() ? tree.next(node.getPath()) : tree.nextFinal(node.getPath());

        // job finished
        if (Objects.isNull(next)) {
            Job.Status statusFromContext = Job.Status.valueOf(job.getContext().get(Variables.JOB_STATUS));
            setJobStatus(job, statusFromContext, execCmd.getError());

            Agent agent = agentService.get(job.getAgentId());
            agentService.tryRelease(agent);

            log.info("Job {} been executed with status {}", job.getId(), statusFromContext);
            return;
        }

        // continue to run next node
        setupNodePathAndDispatch(job, next);
    }

    private VariableMap initJobContext(Flow flow, Long buildNumber, VariableMap... inputs) {
        VariableMap context = new VariableMap(20);
        context.putString(Variables.SERVER_URL, appProperties.getServerAddress());
        context.putString(Variables.FLOW_NAME, flow.getName());
        context.putString(Variables.JOB_BUILD_NUMBER, buildNumber.toString());
        context.putString(Variables.JOB_STATUS, Job.Status.PENDING.name());

        if (Objects.isNull(inputs)) {
            return context;
        }

        for (VariableMap input : inputs) {
            context.merge(input);
        }

        return context;
    }

    private void setupNodePathAndDispatch(Job job, Node next) {
        job.setCurrentPath(next.getPathAsString());
        jobDao.save(job);

        dispatch(job);
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
            setJobStatus(job, Job.Status.QUEUED, null);
            queueTemplate.convertAndSend(jobQueue.getName(), job);
            return job;
        } catch (Throwable e) {
            throw new StatusException("Unable to enqueue the job {0} since {1}", job.getId(), e.getMessage());
        }
    }

    private Job setJobStatus(Job job, Job.Status newStatus, String message) {
        job.setStatus(newStatus);
        job.setMessage(message);
        jobDao.save(job);
        applicationEventPublisher.publishEvent(new JobStatusChangeEvent(this, job));
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
