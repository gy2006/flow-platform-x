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

import com.flowci.core.agent.service.AgentService;
import com.flowci.core.config.ConfigProperties;
import com.flowci.core.domain.Variables;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.helper.ThreadHelper;
import com.flowci.core.job.dao.ExecutedCmdDao;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobNumberDao;
import com.flowci.core.job.dao.JobYmlDao;
import com.flowci.core.job.domain.CmdId;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobNumber;
import com.flowci.core.job.domain.JobYml;
import com.flowci.core.job.event.JobCreatedEvent;
import com.flowci.core.job.event.JobReceivedEvent;
import com.flowci.core.job.event.StatusChangeEvent;
import com.flowci.core.job.manager.CmdManager;
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
import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
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
    private JobYmlDao jobYmlDao;

    @Autowired
    private JobNumberDao jobNumberDao;

    @Autowired
    private ExecutedCmdDao executedCmdDao;

    @Autowired
    private Cache jobTreeCache;

    @Autowired
    private Cache jobStepCache;

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

    @Autowired
    private CmdManager cmdManager;

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
        job.setCreatedBy(currentUserHelper.get().getId());
        job.setBuildNumber(buildNumber);
        job.setCurrentPath(root.getPathAsString());
        job.setAgentSelector(root.getSelector());

        // init job context
        VariableMap defaultContext = initJobContext(flow, buildNumber);
        job.getContext().merge(defaultContext);

        // set expire at
        Instant expireAt = Instant.now().plus(jobProperties.getExpireInSeconds(), ChronoUnit.SECONDS);
        job.setExpireAt(Date.from(expireAt));
        jobDao.save(job);

        // create job yml
        JobYml jobYml = new JobYml(job.getId(), flow.getName(), yml.getRaw());
        jobYmlDao.save(jobYml);

        // init job steps as executed cmd
        NodeTree tree = getTree(job);
        List<ExecutedCmd> steps = new LinkedList<>();
        for (Node node : tree.getOrdered()) {
            CmdId id = cmdManager.createId(job, node);
            steps.add(new ExecutedCmd(id.toString()));
        }
        executedCmdDao.insert(steps);

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
    public List<ExecutedCmd> listSteps(Job job) {
        return jobStepCache.get(job.getId(), () -> {
            NodeTree tree = getTree(job);
            List<Node> nodes = tree.getOrdered();

            List<String> cmdIdsInStr = new ArrayList<>(nodes.size());
            for (Node node : nodes) {
                CmdId cmdId = cmdManager.createId(job, node);
                cmdIdsInStr.add(cmdId.toString());
            }

            return Lists.newArrayList(executedCmdDao.findAllById(cmdIdsInStr));
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
            Cmd cmd = cmdManager.createShellCmd(job, node);
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
        log.debug("Job {} received from queue", job.getId());
        applicationEventPublisher.publishEvent(new JobReceivedEvent(this, job));

        if (!job.isPending()) {
            log.info("Job {} cannot be process since status not pending", job.getId());
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
            log.debug("Job {} been dispatched to agent {}", job.getId(), available.getName());

        } catch (NotFoundException e) {
            // re-enqueue to job while agent not found
            log.debug("Agent not available, job {} retry", job.getId());
            retry(job);
        }
    }

    @Override
    @RabbitListener(queues = "${app.job.callback-queue-name}")
    public void processCallback(ExecutedCmd execCmd) {
        CmdId cmdId = CmdId.parse(execCmd.getId());
        if (Objects.isNull(cmdId)) {
            log.debug("Illegal cmd callback: {}", execCmd.getId());
            return;
        }

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

    private VariableMap initJobContext(Flow flow, Long buildNumber) {
        VariableMap context = new VariableMap();
        context.putString(Variables.SERVER_URL, appProperties.getServerAddress());
        context.putString(Variables.FLOW_NAME, flow.getName());
        context.putString(Variables.JOB_BUILD_NUMBER, buildNumber.toString());
        return context;
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
            throw new StatusException("Unable to enqueue the job {0} since {1}", job.getId(), e.getMessage());
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
