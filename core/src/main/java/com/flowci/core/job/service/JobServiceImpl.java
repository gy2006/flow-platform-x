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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.agent.event.AgentStatusChangeEvent;
import com.flowci.core.agent.service.AgentService;
import com.flowci.core.common.auth.AuthService;
import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.helper.ThreadHelper;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitChannelOperation;
import com.flowci.core.common.rabbit.RabbitOperation;
import com.flowci.core.common.rabbit.RabbitQueueOperation;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.flow.event.FlowOperationEvent;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobItemDao;
import com.flowci.core.job.dao.JobNumberDao;
import com.flowci.core.job.domain.*;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.*;
import com.flowci.core.job.manager.CmdManager;
import com.flowci.core.job.manager.FlowJobQueueManager;
import com.flowci.core.job.manager.YmlManager;
import com.flowci.core.job.util.JobKeyBuilder;
import com.flowci.core.job.util.StatusHelper;
import com.flowci.domain.Agent;
import com.flowci.domain.Agent.Status;
import com.flowci.domain.Cmd;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.VariableMap;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.tree.*;
import groovy.util.ScriptException;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static com.flowci.core.trigger.domain.Variables.GIT_AUTHOR;

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
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobItemDao jobItemDao;

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private ThreadPoolTaskExecutor jobDeleteExecutor;

    @Autowired
    private CmdManager cmdManager;

    @Autowired
    private YmlManager ymlManager;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private AgentService agentService;

    @Autowired
    private StepService stepService;

    @Autowired
    private FlowJobQueueManager flowJobQueueManager;

    @Autowired
    private RabbitQueueOperation callbackQueueManager;

    private final Map<String, JobConsumerHandler> consumeHandlers = new ConcurrentHashMap<>();

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
    public Page<JobItem> list(Flow flow, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, SortByBuildNumber);
        return jobItemDao.findAllByFlowId(flow.getId(), pageable);
    }

    @Override
    public Job create(Flow flow, Yml yml, Trigger trigger, VariableMap input) {
        // verify yml and parse to Node
        Node root = YmlParser.load(flow.getName(), yml.getRaw());

        // create job number
        JobNumber jobNumber = jobNumberDao.increaseBuildNumber(flow.getId());

        // create job
        Job job = new Job();
        job.setKey(JobKeyBuilder.build(flow, jobNumber.getNumber()));
        job.setFlowId(flow.getId());
        job.setTrigger(trigger);
        job.setBuildNumber(jobNumber.getNumber());
        job.setCurrentPath(root.getPathAsString());
        job.setAgentSelector(root.getSelector());

        // init job context
        VariableMap defaultContext = initJobContext(flow, job, root.getEnvironments(), input);
        job.getContext().merge(defaultContext);

        // setup created by form login user or git event author
        if (authService.hasLogin()) {
            job.setCreatedBy(authService.get().getId());
        } else {
            String createdBy = job.getContext().get(GIT_AUTHOR, "Unknown");
            job.setCreatedBy(createdBy);
        }

        // set expire at
        Instant expireAt = Instant.now().plus(jobProperties.getExpireInSeconds(), ChronoUnit.SECONDS);
        job.setExpireAt(Date.from(expireAt));
        jobDao.insert(job);

        // create job yml
        ymlManager.create(flow, job, yml);

        // init job steps as executed cmd
        stepService.init(job);

        eventManager.publish(new JobCreatedEvent(this, job));
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
            return setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
        }
    }

    @Override
    public Job cancel(Job job) {
        // send stop cmd when is running
        if (job.isRunning()) {
            Agent agent = agentService.get(job.getAgentId());
            Cmd killCmd = cmdManager.createKillCmd();

            agentService.dispatch(killCmd, agent);
            logInfo(job, " cancel cmd been send to {}", agent.getName());
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

            eventManager.publish(new JobDeletedEvent(this, flow, numOfJobDeleted));
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

    @EventListener(FlowInitEvent.class)
    public void startJobQueueConsumers(FlowInitEvent event) {
        for (Flow flow : event.getFlows()) {
            startJobConsumer(flow);
        }
    }

    @EventListener(value = ContextRefreshedEvent.class)
    public void startCallbackQueueConsumer(ContextRefreshedEvent event) {
        RabbitChannelOperation.QueueConsumer consumer = callbackQueueManager.createConsumer((message -> {
            if (message == RabbitOperation.Message.STOP_SIGN) {
                return true;
            }

            try {
                ExecutedCmd executedCmd = objectMapper.readValue(message.getBody(), ExecutedCmd.class);
                CmdId cmdId = CmdId.parse(executedCmd.getId());
                log.info("[Callback]: {}-{} = {}", cmdId.getJobId(), cmdId.getNodePath(), executedCmd.getStatus());

                handleCallback(executedCmd);
                return message.sendAck();
            } catch (IOException e) {
                log.error(e.getMessage());
                return false;
            }
        }));

        consumer.start(false);
    }

    @EventListener(value = FlowOperationEvent.class)
    public void onFlowDeleted(FlowOperationEvent event) {
        if (event.isDeletedEvent()) {
            stopJobConsumer(event.getFlow());
            delete(event.getFlow());
        }
    }

    @EventListener(value = FlowOperationEvent.class)
    public void onFlowCreated(FlowOperationEvent event) {
        if (event.isCreatedEvent()) {
            startJobConsumer(event.getFlow());
        }
    }

    @EventListener(value = CreateNewJobEvent.class)
    public void onApplicationEvent(CreateNewJobEvent event) {
        Job job = create(event.getFlow(), event.getYml(), event.getTrigger(), event.getInput());
        start(job);
    }

    @EventListener(value = AgentStatusChangeEvent.class)
    public void notifyToFindAvailableAgent(AgentStatusChangeEvent event) {
        Agent agent = event.getAgent();

        if (agent.getStatus() != Status.IDLE) {
            return;
        }

        if (!agent.hasJob()) {
            return;
        }

        // notify all consumer to find agent
        consumeHandlers.forEach((s, handler) -> handler.resume());
    }

    @EventListener(value = AgentStatusChangeEvent.class)
    public void updateJobAndStep(AgentStatusChangeEvent event) {
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
        setJobStatusAndSave(job, Job.Status.CANCELLED, "Agent unexpected offline");
    }

    //====================================================================
    //        %% Rabbit events
    //====================================================================

    /**
     * Job queue consumer for each flow
     */
    private class JobConsumerHandler implements Function<RabbitChannelOperation.Message, Boolean> {

        private final static long RetryIntervalOnNotFound = 30 * 1000; // 60 seconds

        private final Object lock = new Object();

        @Getter
        private final String queueName;

        // Message.STOP_SIGN will be coming from other thread
        private final AtomicBoolean isStop = new AtomicBoolean(false);

        JobConsumerHandler(String queueName) {
            this.queueName = queueName;
        }

        @Override
        public Boolean apply(RabbitChannelOperation.Message message) {
            if (message == RabbitOperation.Message.STOP_SIGN) {
                log.info("[Job Consumer] {} will be stopped", queueName);
                isStop.set(true);
                resume();
                return true;
            }

            Optional<Job> optional = convert(message);
            if (!optional.isPresent()) {
                return true;
            }

            Job job = optional.get();
            logInfo(job, "received from queue");
            eventManager.publish(new JobReceivedEvent(this, job));

            if (!job.isQueuing()) {
                logInfo(job, "can't handle it since status is not in queuing");
                return false;
            }

            Agent available;

            while ((available = findAvailableAgent(job)) == null) {
                logInfo(job, "waiting for agent...");

                synchronized (lock) {
                    ThreadHelper.wait(lock, RetryIntervalOnNotFound);
                }

                if (isStop.get()) {
                    return false;
                }

                if (isExpired(job)) {
                    setJobStatusAndSave(job, Job.Status.TIMEOUT, null);
                    logInfo(job, "expired");
                    return false;
                }
            }

            dispatch(job, available);
            return message.sendAck();
        }

        void resume() {
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        private Optional<Job> convert(RabbitChannelOperation.Message message) {
            try {
                return Optional.of(objectMapper.readValue(message.getBody(), Job.class));
            } catch (IOException e) {
                return Optional.empty();
            }
        }
    }

    @Override
    public void handleCallback(ExecutedCmd execCmd) {
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
        Agent current = agentService.get(job.getAgentId());

        // job finished
        if (Objects.isNull(next)) {
            Job.Status statusFromContext = Job.Status.valueOf(job.getContext().get(Variables.Job.Status));
            setJobStatusAndSave(job, statusFromContext, execCmd.getError());

            agentService.tryRelease(current);
            logInfo(job, "finished with status {}", statusFromContext);
            return;
        }

        // continue to run next node
        job.setCurrentPath(next.getPathAsString());
        jobDao.save(job);

        log.debug("Send job {} step {} to agent", job.getKey(), node.getName());
        sendToAgent(job, next, current);
    }

    //====================================================================
    //        %% Utils
    //====================================================================

    private void startJobConsumer(Flow flow) {
        String queueName = flow.getQueueName();

        JobConsumerHandler handler = new JobConsumerHandler(queueName);
        consumeHandlers.put(queueName, handler);

        RabbitQueueOperation manager = flowJobQueueManager.create(queueName);
        RabbitOperation.QueueConsumer consumer = manager.createConsumer(queueName, handler);

        // start consumer
        consumer.start(false);
    }

    private void stopJobConsumer(Flow flow) {
        String queueName = flow.getQueueName();

        // remove queue manager and send Message.STOP_SIGN to consumer
        flowJobQueueManager.remove(queueName);

        // resume
        JobConsumerHandler handler = consumeHandlers.get(queueName);
        if (handler != null) {
            handler.resume();
        }

        consumeHandlers.remove(queueName);
    }

    /**
     * Find available agent and dispatch job
     */
    private void dispatch(Job job, Agent available) {
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
        setJobStatusAndSave(job, Job.Status.RUNNING, null);

        // execute condition script
        Boolean executed = executeBeforeCondition(job, next);
        if (!executed) {
            ExecutedCmd executedCmd = stepService.get(job, next);
            handleCallback(executedCmd);
            return;
        }

        // dispatch job to agent queue
        sendToAgent(job, next, available);
    }

    private Agent findAvailableAgent(Job job) {
        Set<String> agentTags = job.getAgentSelector().getTags();
        List<Agent> agents = agentService.find(Status.IDLE, agentTags);

        if (agents.isEmpty()) {
            return null;
        }

        Iterator<Agent> availableList = agents.iterator();

        // try to lock it
        while (availableList.hasNext()) {
            Agent agent = availableList.next();
            agent.setJobId(job.getId());

            if (agentService.tryLock(agent)) {
                return agent;
            }

            availableList.remove();
        }

        return null;
    }

    /**
     * Send step to agent
     */
    private void sendToAgent(Job job, Node node, Agent agent) {
        // set executed cmd step to running
        ExecutedCmd executedCmd = stepService.get(job, node);

        try {
            if (!executedCmd.isRunning()) {
                executedCmd.setStatus(ExecutedCmd.Status.RUNNING);
                stepService.update(job, executedCmd);
            }

            Cmd cmd = cmdManager.createShellCmd(job, node);
            agentService.dispatch(cmd, agent);
            logInfo(job, "send to agent: step={}, agent={}", node.getName(), agent.getName());
        } catch (Throwable e) {
            log.debug("Fail to dispatch job {} to agent {}", job.getId(), agent.getId(), e);

            // set current step to exception
            executedCmd.setStatus(ExecutedCmd.Status.EXCEPTION);
            stepService.update(job, executedCmd);

            // set current job failure
            setJobStatusAndSave(job, Job.Status.FAILURE, e.getMessage());
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

    private Job enqueue(Job job) {
        if (isExpired(job)) {
            setJobStatusAndSave(job, Job.Status.TIMEOUT, null);
            log.debug("[Job: Timeout] {} has expired", job.getKey());
            return job;
        }

        try {
            RabbitQueueOperation manager = flowJobQueueManager.get(job.getQueueName());

            setJobStatusAndSave(job, Job.Status.QUEUED, null);
            byte[] body = objectMapper.writeValueAsBytes(job);

            manager.send(body, job.getPriority());
            logInfo(job, "enqueue");

            return job;
        } catch (Throwable e) {
            throw new StatusException("Unable to enqueue the job {0} since {1}", job.getId(), e.getMessage());
        }
    }

    private Job setJobStatusAndSave(Job job, Job.Status newStatus, String message) {
        job.setStatus(newStatus);
        job.setMessage(message);
        job.getContext().put(Variables.Job.Status, newStatus.name());
        jobDao.save(job);
        eventManager.publish(new JobStatusChangeEvent(this, job));
        return job;
    }

    private void logInfo(Job job, String message, Object ...params) {
        log.info("[Job] " + job.getKey() + " " + message, params);
    }
}
