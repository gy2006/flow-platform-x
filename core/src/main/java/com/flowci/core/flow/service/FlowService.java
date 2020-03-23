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

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Flow.Status;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.flow.domain.StatsType;
import com.flowci.core.job.domain.Job;
import com.flowci.domain.SimpleAuthPair;
import com.flowci.domain.SimpleKeyPair;
import com.flowci.domain.StringVars;

import java.util.List;

/**
 * @author yang
 */
public interface FlowService {

    /**
     * List flows by current user
     */
    List<Flow> list(Status status);

    /**
     * List flows by user id and status
     */
    List<Flow> list(String userId, Status status);

    /**
     * List flows of current user by credential name
     */
    List<Flow> listByCredential(String credentialName);

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
     *
     * @param name flow name
     * @param gitUrl defined git url, can be null
     * @param credential defined credential
     */
    Flow confirm(String name, String gitUrl, String credential);

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
     * Create ssh-rsa credential
     * It will create default credential name: 'flow-{flow name}-ssh-rsa'
     *
     * @return credential name
     */
    String setSshRsaCredential(String name, SimpleKeyPair keyPair);

    /**
     * Create auth credential
     * It will create default credential name: 'flow-{flow name}-auth'
     *
     * @return credential name
     */
    String setAuthCredential(String name, SimpleAuthPair keyPair);


    /**
     * Add users to flow
     */
    void addUsers(Flow flow, String ...userIds);

    /**
     * Remove users from flow
     */
    void removeUsers(Flow flow, String ...userIds);

    /**
     * List users by flow
     */
    List<String> listUsers(Flow flow);
}
