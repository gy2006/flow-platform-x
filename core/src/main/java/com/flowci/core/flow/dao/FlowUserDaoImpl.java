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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class FlowUserDaoImpl implements FlowUserDao {

    @Autowired
    private MongoOperations mongoOps;

    @Override
    public void delete(String flowId) {
        Query q = Query.query(Criteria.where("flowId").is(flowId));
        mongoOps.remove(q, FlowUser.class);
    }

    @Override
    public List<String> findAllFlowsByUserId(String userId) {
        Query q = Query.query(Criteria.where("userId").is(userId));

        q.fields()
            .exclude("_id")
            .exclude("userId")
            .exclude("createdAt")
            .exclude("createdBy");

        List<FlowUser> lists = mongoOps.find(q, FlowUser.class);
        List<String> ids = new LinkedList<>();

        for (FlowUser item : lists) {
            ids.add(item.getFlowId());
        }

        return ids;
    }

    @Override
    public List<FlowUser> findAllUsers(String flowId) {
        Query q = Query.query(Criteria.where("flowId").is(flowId));
        q.fields().exclude("_id");
        return mongoOps.find(q, FlowUser.class);
    }

    @Override
    public void insert(String flowId, Set<String> userIds, String createdBy) {
        List<FlowUser> batch = new ArrayList<>(userIds.size());

        for (String userId : userIds) {
            FlowUser obj = new FlowUser(userId, createdBy);
            obj.setFlowId(flowId);
            batch.add(obj);
        }

        mongoOps.insert(batch, FlowUser.class);
    }

    @Override
    public void remove(String flowId, Set<String> userIds) {
        Query q = Query.query(Criteria.where("flowId").is(flowId));
        q.addCriteria(Criteria.where("userId").in(userIds));
        mongoOps.remove(q, FlowUser.class);
    }

    @Override
    public boolean exist(String flowId, String userId) {
        Query q = new Query();
        q.addCriteria(Criteria.where("flowId").is(flowId));
        q.addCriteria(Criteria.where("userId").is(userId));
        return mongoOps.exists(q, FlowUser.class);
    }
}
