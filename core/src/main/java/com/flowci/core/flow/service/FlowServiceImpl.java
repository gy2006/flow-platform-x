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

package com.flowci.core.flow.service;

import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.helper.CipherHelper;
import com.flowci.core.common.manager.PathManager;
import com.flowci.core.common.manager.SessionManager;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.common.rabbit.RabbitChannelOperation;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.RSACredential;
import com.flowci.core.credential.service.CredentialService;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.FlowUserDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.event.FlowConfirmedEvent;
import com.flowci.core.flow.event.FlowCreatedEvent;
import com.flowci.core.flow.event.FlowDeletedEvent;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.flow.event.GitTestEvent;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.event.CreateNewJobEvent;
import com.flowci.core.trigger.domain.GitPingTrigger;
import com.flowci.core.trigger.domain.GitPushTrigger;
import com.flowci.core.trigger.domain.GitTrigger;
import com.flowci.core.trigger.domain.GitTrigger.GitEvent;
import com.flowci.core.trigger.event.GitHookEvent;
import com.flowci.core.user.event.UserDeletedEvent;
import com.flowci.domain.ObjectWrapper;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.domain.StringVars;
import com.flowci.domain.VarType;
import com.flowci.domain.VarValue;
import com.flowci.domain.Vars;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.exception.StatusException;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.TriggerFilter;
import com.flowci.tree.YmlParser;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class FlowServiceImpl implements FlowService {

    @Autowired
    private String serverAddress;

    @Autowired
    private ThreadPoolTaskExecutor gitTestExecutor;

    @Autowired
    private Path tmpDir;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private FlowUserDao flowUserDao;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private PathManager pathManager;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private Cache<String, List<String>> gitBranchCache;

    @Autowired
    private RabbitChannelOperation jobQueueManager;

    //====================================================================
    //        %% Public function
    //====================================================================

    @Override
    public List<Flow> list(Status status) {
        String userId = sessionManager.getUserId();
        return list(userId, status);
    }

    @Override
    public List<Flow> listByCredential(String credentialName) {
        Credential credential = credentialService.get(credentialName);

        List<Flow> list = list(Status.CONFIRMED);
        Iterator<Flow> iter = list.iterator();

        for (; iter.hasNext(); ) {
            Flow flow = iter.next();
            if (Objects.equals(flow.getCredentialName(), credential.getName())) {
                continue;
            }

            iter.remove();
        }

        return list;
    }

    @Override
    public List<Flow> list(String userId, Status status) {
        List<String> flowIds = flowUserDao.findAllFlowsByUserId(userId);

        if (flowIds.isEmpty()) {
            return Collections.emptyList();
        }

        return flowDao.findAllByIdInAndStatus(flowIds, status);
    }

    @Override
    public Boolean exist(String name) {
        try {
            Flow flow = get(name);
            return flow.getStatus() != Status.PENDING;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public Flow create(String name) {
        if (!NodePath.validate(name)) {
            String message = "Illegal flow name {0}, the length cannot over 100 and '*' ',' is not available";
            throw new ArgumentException(message, name);
        }

        String userId = sessionManager.getUserId();

        Flow flow = flowDao.findByName(name);
        if (flow != null && flow.getStatus() == Status.CONFIRMED) {
            throw new DuplicateException("Flow {0} already exists", name);
        }

        // reuse from pending list
        List<Flow> pending = flowDao.findAllByStatusAndCreatedBy(Status.PENDING, userId);
        flow = pending.size() > 0 ? pending.get(0) : new Flow();

        // set properties
        flow.setName(name);
        flow.setCreatedBy(userId);

        // set default vars
        Vars<VarValue> localVars = flow.getLocally();
        localVars.put(Variables.Flow.Name, VarValue.of(flow.getName(), VarType.STRING, false));
        localVars.put(Variables.Flow.Webhook, VarValue.of(getWebhook(flow.getName()), VarType.HTTP_URL, false));

        try {
            flowDao.save(flow);
            pathManager.create(flow);
            flowUserDao.create(flow.getId());

            addUsers(flow, flow.getCreatedBy());
            createFlowJobQueue(flow);

            eventManager.publish(new FlowCreatedEvent(this, flow));
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("Flow {0} already exists", name);
        } catch (IOException e) {
            flowDao.delete(flow);
            throw new StatusException("Cannot create flow workspace");
        }

        return flow;
    }

    @Override
    public Flow confirm(String name, String gitUrl, String credential) {
        Flow flow = get(name);

        if (flow.getStatus() == Status.CONFIRMED) {
            throw new DuplicateException("Flow {0} has created", name);
        }

        if (!Strings.isNullOrEmpty(gitUrl)) {
            flow.getLocally().put(Variables.Flow.GitUrl, VarValue.of(gitUrl, VarType.GIT_URL, true));
        }

        if (!Strings.isNullOrEmpty(credential)) {
            flow.getLocally().put(Variables.Flow.SSH_RSA, VarValue.of(credential, VarType.STRING, true));
        }

        flow.setStatus(Status.CONFIRMED);
        flowDao.save(flow);

        eventManager.publish(new FlowConfirmedEvent(this, flow));
        return flow;
    }

    @Override
    public Flow get(String name) {
        Flow flow = flowDao.findByName(name);
        if (Objects.isNull(flow)) {
            throw new NotFoundException("Flow {0} is not found", name);
        }
        return flow;
    }

    @Override
    public Flow getById(String id) {
        Optional<Flow> optional = flowDao.findById(id);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Invalid flow id {0}", id);
    }

    @Override
    public Flow delete(String name) {
        Flow flow = get(name);
        flowDao.delete(flow);
        flowUserDao.delete(flow.getId());

        removeFlowJobQueue(flow);
        eventManager.publish(new FlowDeletedEvent(this, flow));
        return flow;
    }

    @Override
    public void update(Flow flow) {
        flow.setUpdatedAt(Date.from(Instant.now()));
        flowDao.save(flow);
    }

    @Override
    public String setSshRsaCredential(String name, SimpleKeyPair pair) {
        Flow flow = get(name);

        String credentialName = "flow-" + flow.getName() + "-ssh-rsa";
        credentialService.createRSA(credentialName, pair);

        return credentialName;
    }

    @Override
    public void testGitConnection(String name, String url, String privateKeyOrCredentialName) {
        final Flow flow = get(name);
        final ObjectWrapper<String> privateKeyWrapper = new ObjectWrapper<>(privateKeyOrCredentialName);

        if (!CipherHelper.RSA.isPrivateKey(privateKeyOrCredentialName)) {
            RSACredential sshRsa = (RSACredential) credentialService.get(privateKeyOrCredentialName);

            if (Objects.isNull(sshRsa)) {
                throw new ArgumentException("Invalid ssh-rsa name");
            }

            privateKeyWrapper.setValue(sshRsa.getPrivateKey());
        }

        gitTestExecutor.execute(() -> {
            GitBranchLoader loader = new GitBranchLoader(flow.getId(), privateKeyWrapper.getValue(), url);
            List<String> branches = loader.load();
            gitBranchCache.put(flow.getId(), branches);
        });
    }

    @Override
    public List<String> listGitBranch(String name) {
        final Flow flow = get(name);

        String gitUrl = flow.getGitUrl();
        String credentialName = flow.getCredentialName();

        if (Strings.isNullOrEmpty(gitUrl) || Strings.isNullOrEmpty(credentialName)) {
            return Collections.emptyList();
        }

        RSACredential sshRsa = (RSACredential) credentialService.get(credentialName);

        if (Objects.isNull(sshRsa)) {
            throw new ArgumentException("Invalid ssh-rsa name");
        }

        return gitBranchCache.get(flow.getId(), (Function<String, List<String>>) flowId -> {
            GitBranchLoader gitTestRunner = new GitBranchLoader(flow.getId(), sshRsa.getPrivateKey(), gitUrl);
            return gitTestRunner.load();
        });
    }

    @Override
    public void addUsers(Flow flow, String... userIds) {
        flowUserDao.insert(flow.getId(), Sets.newHashSet(userIds));
    }

    @Override
    public List<String> listUsers(Flow flow) {
        return flowUserDao.findAllUsers(flow.getId());
    }

    @Override
    public void removeUsers(Flow flow, String... userIds) {
        Set<String> idSet = Sets.newHashSet(userIds);

        if (idSet.contains(flow.getCreatedBy())) {
            throw new ArgumentException("Cannot remove user who create the flow");
        }

        if (idSet.contains(sessionManager.getUserId())) {
            throw new ArgumentException("Cannot remove current user from flow");
        }

        flowUserDao.remove(flow.getId(), idSet);
    }

    //====================================================================
    //        %% Internal events
    //====================================================================

    @EventListener
    public void initJobQueueForFlow(ContextRefreshedEvent ignore) {
        List<Flow> all = flowDao.findAll();

        for (Flow flow : all) {
            createFlowJobQueue(flow);
        }

        eventManager.publish(new FlowInitEvent(this, all));
    }

    @EventListener
    public void deleteUserFromFlow(UserDeletedEvent event) {
        // TODO:
    }

    @EventListener
    public void onGitHookEvent(GitHookEvent event) {
        Flow flow = get(event.getFlow());

        if (event.isPingEvent()) {
            GitPingTrigger ping = (GitPingTrigger) event.getTrigger();

            Flow.WebhookStatus ws = new Flow.WebhookStatus();
            ws.setAdded(true);
            ws.setCreatedAt(ping.getCreatedAt());
            ws.setEvents(ping.getEvents());

            flow.setWebhookStatus(ws);
            update(flow);
        } else {
            Optional<Yml> optional = ymlDao.findById(flow.getId());

            if (!optional.isPresent()) {
                log.warn("No available yml for flow {}", flow.getName());
                return;
            }

            Yml yml = optional.get();
            Node root = YmlParser.load(flow.getName(), yml.getRaw());

            if (!canStartJob(root, event.getTrigger())) {
                log.debug("Cannot start job since filter not matched on flow {}", flow.getName());
                return;
            }

            StringVars gitInput = event.getTrigger().toVariableMap();
            Trigger jobTrigger = event.getTrigger().toJobTrigger();

            eventManager.publish(new CreateNewJobEvent(this, flow, yml, jobTrigger, gitInput));
        }
    }

    //====================================================================
    //        %% Utils
    //====================================================================

    private boolean canStartJob(Node root, GitTrigger trigger) {
        TriggerFilter condition = root.getTrigger();

        if (trigger.getEvent() == GitEvent.PUSH) {
            GitPushTrigger pushTrigger = (GitPushTrigger) trigger;
            return condition.isMatchBranch(pushTrigger.getRef());
        }

        if (trigger.getEvent() == GitEvent.TAG) {
            GitPushTrigger tagTrigger = (GitPushTrigger) trigger;
            return condition.isMatchTag(tagTrigger.getRef());
        }

        return true;
    }

    private void createFlowJobQueue(Flow flow) {
        jobQueueManager.declare(flow.getQueueName(), true, 255);
    }

    private void removeFlowJobQueue(Flow flow) {
        jobQueueManager.delete(flow.getQueueName());
    }

    private String getWebhook(String name) {
        return serverAddress + "/webhooks/" + name;
    }

    private class GitBranchLoader {

        private static final String RefPrefix = "refs/heads/";

        private final String flowId;

        private final String privateKey;

        private final String url;

        GitBranchLoader(String flowId, String privateKey, String url) {
            this.flowId = flowId;
            this.privateKey = privateKey;
            this.url = url;
        }

        public List<String> load() {
            List<String> branches = new LinkedList<>();

            try (PrivateKeySessionFactory sessionFactory = new PrivateKeySessionFactory(privateKey)) {
                // publish FETCHING event
                eventManager.publish(new GitTestEvent(this, flowId));

                Collection<Ref> refs = Git.lsRemoteRepository()
                    .setRemote(url)
                    .setHeads(true)
                    .setTransportConfigCallback(transport -> {
                        SshTransport sshTransport = (SshTransport) transport;
                        sshTransport.setSshSessionFactory(sessionFactory);
                    })
                    .setTimeout(20)
                    .call();

                for (Ref ref : refs) {
                    String refName = ref.getName();
                    branches.add(refName.substring(RefPrefix.length()));
                }

                // publish DONE event
                eventManager.publish(new GitTestEvent(this, flowId, branches));

            } catch (IOException | GitAPIException e) {
                // publish ERROR event
                eventManager.publish(new GitTestEvent(this, flowId, e.getMessage()));
            }

            return branches;
        }
    }

    private class PrivateKeySessionFactory extends JschConfigSessionFactory implements AutoCloseable {

        private Path tmpPrivateKeyFile = Paths.get(tmpDir.toString(), UUID.randomUUID().toString());

        PrivateKeySessionFactory(String privateKey) throws IOException {
            Files.write(tmpPrivateKeyFile, privateKey.getBytes());
        }

        @Override
        protected void configure(Host host, Session session) {
            session.setConfig("StrictHostKeyChecking", "no");
        }

        @Override
        protected JSch createDefaultJSch(FS fs) throws JSchException {
            JSch defaultJSch = super.createDefaultJSch(fs);
            defaultJSch.addIdentity(tmpPrivateKeyFile.toString());
            return defaultJSch;
        }

        @Override
        public void close() throws IOException {
            Files.deleteIfExists(tmpPrivateKeyFile);
        }
    }
}
