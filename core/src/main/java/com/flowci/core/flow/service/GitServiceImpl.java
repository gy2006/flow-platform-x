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

import com.flowci.core.common.git.GitClient;
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
import com.flowci.exception.NotAvailableException;
import com.flowci.util.StringHelper;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.base.Function;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

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
    private Path repoDir;

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
        eventManager.publish(new GitTestEvent(this, flow.getId()));
        GitClient client = new GitClient(url, tmpDir, credential.toSimpleSecret());

        try {
            final List<String> branches = client.branches();
            eventManager.publish(new GitTestEvent(this, flow.getId(), branches));
            return branches;
        } catch (Exception e) {
            log.warn(e.getMessage());
            eventManager.publish(new GitTestEvent(this, flow.getId(), e.getMessage()));
            return Collections.emptyList();
        }
    }
}
