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

import com.flowci.core.common.config.ConfigProperties;
import com.flowci.core.common.domain.Variables;
import com.flowci.core.common.rabbit.RabbitChannelOperation;
import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.RSAKeyPair;
import com.flowci.core.credential.service.CredentialService;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.YmlDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.event.FlowInitEvent;
import com.flowci.core.flow.event.FlowOperationEvent;
import com.flowci.core.flow.event.GitTestEvent;
import com.flowci.core.user.CurrentUserHelper;
import com.flowci.domain.VariableMap;
import com.flowci.exception.AccessException;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.Node;
import com.flowci.tree.NodePath;
import com.flowci.tree.YmlParser;
import com.flowci.util.CipherHelper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class FlowServiceImpl implements FlowService {

    @Autowired
    private ConfigProperties appProperties;

    @Autowired
    private ThreadPoolTaskExecutor gitTestExecutor;

    @Autowired
    private Path tmpDir;

    @Autowired
    private CurrentUserHelper currentUserHelper;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private YmlDao ymlDao;

    @Autowired
    private CronService cronService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private Template defaultYmlTemplate;

    @Autowired
    private Cache<String, List<String>> gitBranchCache;

    @Autowired
    private RabbitChannelOperation jobQueueManager;

    @EventListener
    public void onInit(ContextRefreshedEvent ignore) {
        List<Flow> all = flowDao.findAll();

        for (Flow flow : all) {
            createFlowJobQueue(flow);
        }

        eventManager.publish(new FlowInitEvent(this, all));
    }

    @Override
    public List<Flow> list(Status status) {
        String userId = currentUserHelper.get().getId();
        return flowDao.findAllByStatusAndCreatedBy(status, userId);
    }

    @Override
    public List<Flow> listByCredential(String credentialName) {
        Credential credential = credentialService.get(credentialName);

        List<Flow> list = list(Status.CONFIRMED);
        Iterator<Flow> iter = list.iterator();

        for (; iter.hasNext(); ) {
            Flow flow = iter.next();
            String value = flow.getVariables().get(Variables.Flow.SSH_RSA);

            if (Objects.equals(value, credential.getName())) {
                continue;
            }

            iter.remove();
        }

        return list;
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

        String userId = currentUserHelper.get().getId();

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

        VariableMap vars = flow.getVariables();
        vars.put(Variables.App.Url, appProperties.getServerAddress());
        vars.put(Variables.Flow.Name, name);
        vars.put(Variables.Flow.Webhook, getWebhook(name));

        flowDao.save(flow);

        createFlowJobQueue(flow);

        eventManager.publish(new FlowOperationEvent(this, flow, FlowOperationEvent.Operation.CREATED));

        return flow;
    }

    @Override
    public Flow confirm(String name) {
        Flow flow = get(name);

        if (flow.getStatus() == Status.CONFIRMED) {
            throw new NotAvailableException("Flow {0} is created", name);
        }

        flow.setStatus(Status.CONFIRMED);
        flowDao.save(flow);

        // create template yml
        String templateYml = getTemplateYml(flow);
        saveYml(flow, templateYml);

        return flow;
    }

    @Override
    public Flow get(String name) {
        Flow flow = flowDao.findByNameAndCreatedBy(name, currentUserHelper.get().getId());
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

        try {
            Yml yml = getYml(flow);
            ymlDao.delete(yml);
        } catch (NotFoundException ignore) {

        }

        removeFlowJobQueue(flow);
        eventManager.publish(new FlowOperationEvent(this, flow, FlowOperationEvent.Operation.DELETED));
        return flow;
    }

    @Override
    public void update(Flow flow) {
        verifyFlowIdAndUser(flow);
        flowDao.save(flow);
    }

    @Override
    public String getTemplateYml(Flow flow) {
        VelocityContext context = new VelocityContext();
        for (Map.Entry<String, String> entry : flow.getVariables().entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }

        try (StringWriter sw = new StringWriter()) {
            defaultYmlTemplate.merge(context, sw);
            return sw.toString();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Yml getYml(Flow flow) {
        verifyFlowIdAndUser(flow);
        Optional<Yml> optional = ymlDao.findById(flow.getId());
        if (optional.isPresent()) {
            return optional.get();
        }
        throw new NotFoundException("No yml defined for flow {0}", flow.getName());
    }

    @Override
    public Yml saveYml(Flow flow, String yml) {
        verifyFlowIdAndUser(flow);

        if (Strings.isNullOrEmpty(yml)) {
            throw new ArgumentException("Yml content cannot be null or empty");
        }

        YmlParser.load(flow.getName(), yml);
        Yml ymlObj = new Yml(flow.getId(), yml);
        ymlObj.setCreatedBy(currentUserHelper.get().getId());
        ymlDao.save(ymlObj);

        Node node = YmlParser.load(flow.getName(), ymlObj.getRaw());

        // sync flow envs from yml root envs
        flow.getVariables().merge(node.getEnvironments());
        flowDao.save(flow);

        // update cron task
        cronService.update(flow, node, ymlObj);
        return ymlObj;
    }

    @Override
    public void setSshRsaCredential(String name, RSAKeyPair keyPair) {
        Flow flow = get(name);

        String credentialName = "flow-" + flow.getName() + "-ssh-rsa";
        credentialService.createRSA(credentialName, keyPair.getPublicKey(), keyPair.getPrivateKey());

        flow.getVariables().put(Variables.Flow.SSH_RSA, credentialName);
        update(flow);
    }

    @Override
    public void testGitConnection(String name, String url, String privateKeyOrCredentialName) {
        final Flow flow = get(name);
        String privateKey = privateKeyOrCredentialName;

        if (!CipherHelper.isRsaPrivateKey(privateKeyOrCredentialName)) {
            RSAKeyPair sshRsa = (RSAKeyPair) credentialService.get(privateKeyOrCredentialName);

            if (Objects.isNull(sshRsa)) {
                throw new ArgumentException("Invalid ssh-rsa name");
            }

            privateKey = sshRsa.getPrivateKey();
        }

        gitTestExecutor.execute(new GitTestRunner(flow.getId(), privateKey, url));
    }

    @Override
    public List<String> listGitBranch(String name) {
        final Flow flow = get(name);

        String gitUrl = flow.getVariables().get(Variables.Flow.GitUrl);
        String credentialName = flow.getVariables().get(Variables.Flow.SSH_RSA);

        if (Strings.isNullOrEmpty(gitUrl) || Strings.isNullOrEmpty(credentialName)) {
            return Collections.emptyList();
        }

        RSAKeyPair sshRsa = (RSAKeyPair) credentialService.get(credentialName);

        if (Objects.isNull(sshRsa)) {
            throw new ArgumentException("Invalid ssh-rsa name");
        }

        return gitBranchCache.get(flow.getId(), (Function<String, List<String>>) flowId -> {
            GitTestRunner gitTestRunner = new GitTestRunner(flow.getId(), sshRsa.getPrivateKey(), gitUrl);
            gitTestRunner.run();
            return gitTestRunner.getBranches();
        });
    }

    private void createFlowJobQueue(Flow flow) {
        jobQueueManager.declare(flow.getQueueName(), true, 255);
    }

    private void removeFlowJobQueue(Flow flow) {
        jobQueueManager.delete(flow.getQueueName());
    }

    private String getWebhook(String name) {
        return appProperties.getServerAddress() + "/webhooks/" + name;
    }

    private void verifyFlowIdAndUser(Flow flow) {
        String flowId = flow.getId();
        if (Strings.isNullOrEmpty(flowId)) {
            throw new ArgumentException("The flow id is missing");
        }

        if (!Objects.equals(flow.getCreatedBy(), currentUserHelper.get().getId())) {
            throw new AccessException("Illegal account for flow {0}", flow.getName());
        }
    }

    private class GitTestRunner implements Runnable {

        private final String flowId;

        private final String privateKey;

        private final String url;

        @Getter
        private final List<String> branches = new LinkedList<>();

        GitTestRunner(String flowId, String privateKey, String url) {
            this.flowId = flowId;
            this.privateKey = privateKey;
            this.url = url;
        }

        @Override
        public void run() {
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
                    .call();

                for (Ref ref : refs) {
                    String refName = ref.getName();
                    branches.add(refName.substring(refName.lastIndexOf("/") + 1));
                }

                // add to cache
                gitBranchCache.put(flowId, branches);

                // publish DONE event
                eventManager.publish(new GitTestEvent(this, flowId, branches));

            } catch (IOException | GitAPIException e) {
                // publish ERROR event
                eventManager.publish(new GitTestEvent(this, flowId, e.getMessage()));
            }
        }
    }

    private class PrivateKeySessionFactory extends JschConfigSessionFactory implements AutoCloseable {

        private Path tmpPrivateKeyFile = Paths.get(tmpDir.toString(), UUID.randomUUID().toString());

        PrivateKeySessionFactory(String privateKey) throws IOException {
            Files.write(tmpPrivateKeyFile, privateKey.getBytes());
        }

        @Override
        protected void configure(Host host, Session session) {
            // do nothing
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
