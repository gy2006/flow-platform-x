/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.flow.dao;

import com.flowci.core.flow.domain.FlowUser;

import java.util.List;

public interface FlowUserListDao {

    /**
     * Create empty flow user list
     */
    void create(String flowId);

    /**
     * Remove flow user list
     */
    void delete(String flowId);

    /**
     * Find all flows by users id
     */
    List<String> findAllFlowsByUserId(String userId);

    /**
     * Find all users by flow id
     */
    List<FlowUser> findAllUsers(String flowId);

    /**
     * Batch insert users
     */
    boolean insert(String flowId, FlowUser...users);

    /**
     * Batch remove users
     */
    boolean remove(String flowId, String ...userId);

    /**
     * Check user is existed
     */
    boolean exist(String flowId, String userId);
}