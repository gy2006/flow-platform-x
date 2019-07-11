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

import com.flowci.core.credential.domain.RSAKeyPair;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.Yml;
import java.util.List;

/**
 * @author yang
 */
public interface FlowService {

    /**
     * List all flow by current user
     */
    List<Flow> list(Status status);

    /**
     * Check the flow name is existed
     */
    Boolean exist(String name);

    /**
     * Create flow by name with pending status
     */
    Flow create(String name);

    /**
     * Confirm flow
     */
    Flow confirm(String name);

    /**
     * Get flow by name
     */
    Flow get(String name);

    /**
     * Get flow by id
     */
    Flow getById(String id);

    /**
     * Delete flow and yml
     */
    Flow delete(String name);

    /**
     * Update flow name or variables
     */
    void update(Flow flow);

    /**
     * Get default template yml of flow
     */
    String getTemplateYml(Flow flow);

    /**
     * Get yml by flow
     */
    Yml getYml(Flow flow);

    /**
     * Create or update yml for flow
     */
    Yml saveYml(Flow flow, String yml);

    /**
     * Create ssh-rsa credential
     * It will create default credential name: 'flow-{flow name}-ssh-rsa'
     */
    void setSshRsaCredential(String name, RSAKeyPair keyPair);

    /**
     * Test git connection for flow and dispatch application event
     */
    void testGitConnection(String name, String url, String privateKeyOrCredentialName);

    /**
     * List remote branches
     * @return list of branches or empty list if git config not defined
     */
    List<String> listGitBranch(String name);
}
