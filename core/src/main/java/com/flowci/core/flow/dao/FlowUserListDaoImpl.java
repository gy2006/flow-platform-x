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
import com.flowci.core.flow.domain.FlowUserList;
import com.mongodb.client.result.UpdateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@Repository
public class FlowUserListDaoImpl implements FlowUserListDao {

    @Autowired
    private MongoOperations mongoOps;

    @Override
    public void create(String flowId) {
        mongoOps.insert(new FlowUserList(flowId));
    }

    @Override
    public void delete(String flowId) {
        mongoOps.remove(new FlowUserList(flowId));
    }

    @Override
    public List<String> findAllFlowsByUserId(String userId) {
        Query q = Query.query(Criteria.where("users")
                .elemMatch(Criteria.where("userId").is(userId)));

        q.fields().exclude("users");

        List<FlowUserList> lists = mongoOps.find(q, FlowUserList.class);
        List<String> ids = new LinkedList<>();

        for (FlowUserList item : lists) {
            ids.add(item.getFlowId());
        }

        return ids;
    }

    @Override
    public List<FlowUser> findAllUsers(String flowId) {
        Query q = Query.query(Criteria.where("_id").is(flowId));
        FlowUserList flowUsers = mongoOps.findOne(q, FlowUserList.class);
        if (Objects.isNull(flowUsers)) {
            return Collections.emptyList();
        }
        return flowUsers.getUsers();
    }

    @Override
    public boolean insert(String flowId, FlowUser... users) {
        Query q = Query.query(Criteria.where("_id").is(flowId));
        Update u = new Update().addToSet("users").each(users);

        UpdateResult result = mongoOps.updateFirst(q, u, FlowUserList.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean remove(String flowId, String... userIds) {
        Query q = Query.query(Criteria.where("_id").is(flowId));
        Update u = new Update().pull("users", Query.query(Criteria.where("userId").in(userIds)));

        UpdateResult result = mongoOps.updateFirst(q, u, FlowUserList.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean exist(String flowId, String userId) {
        Query q = new Query();
        q.addCriteria(Criteria.where("_id").is(flowId));
        q.addCriteria(Criteria.where("users").elemMatch(Criteria.where("userId").is(userId)));
        return mongoOps.exists(q, FlowUserList.class);
    }
}
