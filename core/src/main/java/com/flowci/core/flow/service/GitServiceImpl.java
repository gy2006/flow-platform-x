/*
 * Copyright 2019 flow.ci
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

import com.flowci.core.common.manager.SpringEventManager;
import com.flowci.core.credential.domain.AuthCredential;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.domain.Credential.Category;
import com.flowci.core.credential.domain.RSACredential;
import com.flowci.core.credential.service.CredentialService;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.event.GitTestEvent;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.exception.ArgumentException;
import com.flowci.util.StringHelper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Function;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class GitServiceImpl implements GitService {

    @Autowired
    private ThreadPoolTaskExecutor gitTestExecutor;

    @Autowired
    private Path tmpDir;

    @Autowired
    private Cache<String, List<String>> gitBranchCache;

    @Autowired
    private SpringEventManager eventManager;

    @Autowired
    private CredentialService credentialService;

    @Override
    public void testConn(Flow flow, String url, String credential) {
        Credential c = getCredential(credential);

        if (c != null) {
            if (c.getCategory() != Category.AUTH && StringHelper.isHttpLink(url)) {
                throw new ArgumentException("Invalid credential for http git url");
            }
        }

        gitTestExecutor.execute(() -> fetchBranchFromGit(flow, url, c));
    }

    @Override
    public void testConn(Flow flow, String url, SimpleKeyPair rsa) {
        if (StringHelper.isHttpLink(url)) {
            throw new ArgumentException("Invalid git url");
        }

        RSACredential c = new RSACredential();
        c.setPair(rsa);

        gitTestExecutor.execute(() -> fetchBranchFromGit(flow, url, c));
    }

    @Override
    public void testConn(Flow flow, String url, SimpleAuthPair auth) {
        if (!StringHelper.isHttpLink(url)) {
            throw new ArgumentException("Invalid git url");
        }

        AuthCredential c = new AuthCredential();
        c.setPair(auth);

        gitTestExecutor.execute(() -> fetchBranchFromGit(flow, url, c));
    }

    @Override
    public List<String> listGitBranch(Flow flow) {
        final String gitUrl = flow.getGitUrl();
        final String credentialName = flow.getCredentialName();
        final Credential c = getCredential(credentialName);

        return gitBranchCache.get(flow.getId(), (Function<String, List<String>>) flowId ->
            fetchBranchFromGit(flow, gitUrl, c));
    }


    //====================================================================
    //        %% Utils
    //====================================================================

    private Credential getCredential(String name) {
        if (!StringHelper.hasValue(name)) {
            return null;
        }

        return credentialService.get(name);
    }

    private List<String> fetchBranchFromGit(Flow flow, String url, Credential credential) {
        if (Objects.isNull(credential)) {
            return fetchBranchViaHttp(flow, url, null);
        }

        if (credential.getCategory() == Credential.Category.SSH_RSA) {
            return fetchBranchViaSSH(flow, url, (RSACredential) credential);
        }

        if (credential.getCategory() == Credential.Category.AUTH) {
            return fetchBranchViaHttp(flow, url, (AuthCredential) credential);
        }

        throw new UnsupportedOperationException("Unsupported credential category");

    }

    private List<String> fetchBranchViaHttp(Flow flow, String url, AuthCredential credential) {
        try (GitBranchLoader loader = new HttpGitBranchLoader(flow.getId(), url, credential)) {
            return loader.load();
        } catch (Exception errorOnClose) {
            return Collections.emptyList();
        }
    }

    private List<String> fetchBranchViaSSH(Flow flow, String url, RSACredential credential) {
        try (GitBranchLoader loader = new SshGitBranchLoader(flow.getId(), url, credential)) {
            return loader.load();
        } catch (Exception errorOnClose) {
            return Collections.emptyList();
        }
    }

    //====================================================================
    //        %% Inner Classes
    //====================================================================

    private abstract class GitBranchLoader implements AutoCloseable {

        private static final String RefPrefix = "refs/heads/";

        protected final String flowId;

        protected final String url;

        GitBranchLoader(String flowId, String url) {
            this.flowId = flowId;
            this.url = url;
        }

        abstract void setup(LsRemoteCommand command) throws Throwable;

        List<String> load() {
            // publish FETCHING event
            eventManager.publish(new GitTestEvent(this, flowId));
            LsRemoteCommand command = Git.lsRemoteRepository()
                .setRemote(url)
                .setHeads(true)
                .setTimeout(20);

            try {
                setup(command);
                Collection<Ref> refs = command.call();
                List<String> branches = new LinkedList<>();

                for (Ref ref : refs) {
                    String refName = ref.getName();
                    branches.add(refName.substring(RefPrefix.length()));
                }

                // publish DONE event
                eventManager.publish(new GitTestEvent(this, flowId, branches));
                return branches;
            } catch (Throwable e) {
                // publish ERROR event
                log.warn(e.getMessage());
                eventManager.publish(new GitTestEvent(this, flowId, e.getMessage()));
                return Collections.emptyList();
            }
        }
    }

    private class SshGitBranchLoader extends GitBranchLoader {

        private final String privateKey;

        private PrivateKeySessionFactory sessionFactory;

        SshGitBranchLoader(String flowId, String url, RSACredential credential) {
            super(flowId, url);
            this.privateKey = Objects.isNull(credential) ? StringHelper.EMPTY : credential.getPrivateKey();
        }

        @Override
        void setup(LsRemoteCommand command) throws Throwable {
            this.sessionFactory = new PrivateKeySessionFactory(privateKey);

            command.setTransportConfigCallback(transport -> {
                SshTransport sshTransport = (SshTransport) transport;
                sshTransport.setSshSessionFactory(sessionFactory);
            });
        }

        @Override
        public void close() throws Exception {
            if (Objects.isNull(sessionFactory)) {
                return;
            }

            sessionFactory.close();
        }
    }

    private class HttpGitBranchLoader extends GitBranchLoader {

        private final AuthCredential credential;

        HttpGitBranchLoader(String flowId, String url, AuthCredential credential) {
            super(flowId, url);
            this.credential = credential;
        }

        @Override
        void setup(LsRemoteCommand command) throws Throwable {
            if (Objects.isNull(credential)) {
                return;
            }

            UsernamePasswordCredentialsProvider provider = new UsernamePasswordCredentialsProvider(
                credential.getUsername(), credential.getPassword());
            command.setCredentialsProvider(provider);
        }

        @Override
        public void close() throws Exception {

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
