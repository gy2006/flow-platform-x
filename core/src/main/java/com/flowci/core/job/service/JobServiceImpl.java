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

import static com.flowci.core.trigger.domain.Variables.GIT_AUTHOR;

import com.flowci.core.agent.event.StatusChangeEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.manager.QueueManager;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobNumberDao;
import com.flowci.core.job.domain.CmdId;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobNumber;
import com.flowci.core.job.domain.JobYml;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.job.event.JobDeletedEvent;
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
import com.flowci.tree.GroovyRunner;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.NodeTree;
import com.flowci.tree.YmlParser;
import groovy.util.ScriptException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
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

    private static final Integer DefaultBeforeTimeout = 5;

    //====================================================================
    //        %% Spring injection
    //====================================================================

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
    private Queue jobQueue;

    @Autowired
    private ThreadPoolTaskExecutor retryExecutor;

    @Autowired
    private ThreadPoolTaskExecutor jobDeleteExecutor;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private QueueManager queueManager;

    @Autowired
    private AgentService agentService;

    @Autowired
    private StepService stepService;

    //====================================================================
    //        %% Public function
    //====================================================================

    @Override
    public Job get(String jobId) {
        Optional<Job> job = jobDao.findById(jobId);

        if (job.isPresent()) {
            return job.get();
        }

        throw new NotFoundException("Job '{}' not found", jobId);
    }

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
        Optional<JobNumber> optional = jobNumberDao.findById(flow.getId());

        if (optional.isPresent()) {
            JobNumber latest = optional.get();
            return get(flow, latest.getNumber());
        }

        throw new NotFoundException("No jobs for flow {0}", flow.getName());
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
        VariableMap defaultContext = initJobContext(flow, job, root.getEnvironments(), input);
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
    public void delete(Flow flow) {
        jobDeleteExecutor.execute(() -> {
            jobNumberDao.deleteByFlowId(flow.getId());
            log.info("Deleted: job number of flow {}", flow.getName());

            Long numOfJobDeleted = jobDao.deleteByFlowId(flow.getId());
            log.info("Deleted: {} jobs of flow {}", numOfJobDeleted, flow.getName());

            Long numOfStepDeleted = stepService.delete(flow.getId());
            log.info("Deleted: {} steps of flow {}", numOfStepDeleted, flow.getName());

            applicationEventPublisher.publishEvent(new JobDeletedEvent(this, flow, numOfJobDeleted));
        });
    }

    @Override
    public boolean isExpired(Job job) {
        Instant expireAt = job.getExpireAt().toInstant();
        return Instant.now().compareTo(expireAt) == 1;
    }

    //====================================================================
    //        %% Internal events
    //====================================================================

    @EventListener(value = CreateNewJobEvent.class)
    public void onApplicationEvent(CreateNewJobEvent event) {
        Job job = create(event.getFlow(), event.getYml(), event.getTrigger(), event.getInput());
        start(job);
    }

    @EventListener(value = StatusChangeEvent.class)
    public void onApplicationEvent(StatusChangeEvent event) {
        Agent agent = event.getAgent();
        if (agent.getStatus() != Status.OFFLINE || Objects.isNull(agent.getJobId())) {
            return;
        }

        Job job = get(agent.getJobId());
        if (job.isDone()) {
            return;
        }

        // update step status
        List<ExecutedCmd> steps = stepService.list(job);
        for (ExecutedCmd step : steps) {
            if (step.isRunning() || step.isPending()) {
                step.setStatus(ExecutedCmd.Status.SKIPPED);
                stepService.update(job, step);
            }
        }

        // update job status
        setJobStatus(job, Job.Status.CANCELLED, "Agent unexpected offline");
    }

    //====================================================================
    //        %% Rabbit events
    //====================================================================

    @Override
    @RabbitListener(queues = "${app.job.queue-name}", containerFactory = "jobAndCallbackContainerFactory")
    public void handleJob(Job job) {
        log.debug("Job {} received from queue", job.getId());
        applicationEventPublisher.publishEvent(new JobReceivedEvent(this, job));

        if (!job.isQueuing()) {
            log.info("Job {} cannot be process since status not queuing", job.getId());
            return;
        }

        dispatch(job);
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
        Job job = get(cmdId.getJobId());
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

        // setup current job status if not tail node
        if (!node.isTail()) {
            context.put(Variables.Job.Status, StatusHelper.convert(execCmd).name());
        }

        jobDao.save(job);

        // find next node
        Node next = findNext(job, tree, node, execCmd.isSuccess());

        // job finished
        if (Objects.isNull(next)) {
            Job.Status statusFromContext = Job.Status.valueOf(job.getContext().get(Variables.Job.Status));
            setJobStatus(job, statusFromContext, execCmd.getError());

            Agent agent = agentService.get(job.getAgentId());
            agentService.tryRelease(agent);

            log.info("Job {} been executed with status {}", job.getId(), statusFromContext);
            return;
        }

        // continue to run next node
        job.setCurrentPath(next.getPathAsString());
        jobDao.save(job);

        log.debug("Send job {} step {} to agent", job.getKey(), node.getName());
        sendToAgent(job, next);
    }

    //====================================================================
    //        %% Utils
    //====================================================================

    /**
     * Find available agent and dispatch job
     */
    private void dispatch(Job job) {
        // find available agents
        Set<String> agentTags = job.getAgentSelector().getTags();
        List<Agent> agents = agentService.find(Status.IDLE, agentTags);

        // re-enqueue to job while agent not found
        if (agents.isEmpty()) {
            log.debug("Agent not available, job {} retry", job.getId());
            retry(job);
            return;
        }

        Boolean isLocked = Boolean.FALSE;
        Agent available = null;
        Iterator<Agent> availableList = agents.iterator();

        // try to lock it
        while (availableList.hasNext()) {
            Agent agent = availableList.next();
            agent.setJobId(job.getId());

            isLocked = agentService.tryLock(agent);
            if (isLocked) {
                available = agent;
                break;
            }

            availableList.remove();
        }

        // re-enqueue to job while agent been locked by other
        if (!isLocked) {
            log.debug("Agent not found for job {}, put into the retrying queue", job.getId());
            retry(job);
            return;
        }

        NodeTree tree = ymlManager.getTree(job);
        Node next = tree.next(currentNodePath(job));

        // do not accept job without regular steps
        if (Objects.isNull(next)) {
            log.debug("Next node cannot be found when process job {}", job);
            return;
        }

        log.debug("Next step of job {} is {}", job.getId(), next.getName());

        // set path, agent id, and status to job
        job.setCurrentPath(next.getPathAsString());
        job.setAgentId(available.getId());
        setJobStatus(job, Job.Status.RUNNING, null);

        // execute condition script
        Boolean executed = executeBeforeCondition(job, next);
        if (!executed) {
            ExecutedCmd executedCmd = stepService.get(job, next);
            processCallback(executedCmd);
            return;
        }

        // dispatch job to agent queue
        sendToAgent(job, next);
    }

    /**
     * Send step to agent
     */
    private void sendToAgent(Job job, Node node) {
        Agent agent = agentService.get(job.getAgentId());

        // set executed cmd step to running
        ExecutedCmd executedCmd = stepService.get(job, node);

        try {
            if (!executedCmd.isRunning()) {
                executedCmd.setStatus(ExecutedCmd.Status.RUNNING);
                stepService.update(job, executedCmd);
            }

            Cmd cmd = cmdManager.createShellCmd(job, node);
            agentService.dispatch(cmd, agent);
            log.debug("Job {} with cmd {} been dispatched to agent {}", job.getId(), cmd.getId(), agent.getId());
        } catch (Throwable e) {
            log.debug("Fail to dispatch job {} to agent {}", job.getId(), agent.getId(), e);

            // set current step to exception
            executedCmd.setStatus(ExecutedCmd.Status.EXCEPTION);
            stepService.update(job, executedCmd);

            // set current job failure
            setJobStatus(job, Job.Status.FAILURE, e.getMessage());

            agentService.tryRelease(agent);
        }
    }

    private Node findNext(Job job, NodeTree tree, Node current, boolean isSuccess) {
        Node next = isSuccess ? tree.next(current.getPath()) : tree.nextFinal(current.getPath());

        if (Objects.isNull(next)) {
            return null;
        }

        // Execute before condition to check the next node should be skipped or not
        if (executeBeforeCondition(job, next)) {
            return next;
        }

        return findNext(job, tree, next, true);
    }

    private Boolean executeBeforeCondition(Job job, Node node) {
        if (!node.hasBefore()) {
            return true;
        }

        VariableMap map = VariableMap.merge(job.getContext(), node.getEnvironments());

        try {
            GroovyRunner<Boolean> runner = GroovyRunner.create(DefaultBeforeTimeout, node.getBefore(), map);
            Boolean result = runner.run();

            if (Objects.isNull(result) || result == Boolean.FALSE) {
                ExecutedCmd executedCmd = stepService.get(job, node);
                executedCmd.setStatus(ExecutedCmd.Status.SKIPPED);
                executedCmd.setError("The 'before' condition cannot be matched");
                stepService.update(job, executedCmd);
                return false;
            }

            return true;
        } catch (ScriptException e) {
            ExecutedCmd executedCmd = stepService.get(job, node);
            executedCmd.setStatus(ExecutedCmd.Status.SKIPPED);
            executedCmd.setError(e.getMessage());
            stepService.update(job, executedCmd);
            return false;
        }
    }

    private VariableMap initJobContext(Flow flow, Job job, VariableMap... inputs) {
        VariableMap context = new VariableMap(flow.getVariables());
        context.put(Variables.Job.Trigger, job.getTrigger().toString());
        context.put(Variables.Job.BuildNumber, job.getBuildNumber().toString());
        context.put(Variables.Job.Status, Job.Status.PENDING.name());

        if (Objects.isNull(inputs)) {
            return context;
        }

        for (VariableMap input : inputs) {
            if (Objects.isNull(input)) {
                continue;
            }
            context.merge(input);
        }

        return context;
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
            queueManager.send(jobQueue.getName(), job, 255);
            return job;
        } catch (Throwable e) {
            throw new StatusException("Unable to enqueue the job {0} since {1}", job.getId(), e.getMessage());
        }
    }

    private Job setJobStatus(Job job, Job.Status newStatus, String message) {
        job.setStatus(newStatus);
        job.setMessage(message);
        job.getContext().put(Variables.Job.Status, newStatus.name());
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
