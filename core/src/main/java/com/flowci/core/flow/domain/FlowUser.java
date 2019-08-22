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

package com.flowci.core.flow.domain;

import java.time.Instant;
import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@EqualsAndHashCode(of = {"userId"})
@Document(collection = "flow_user")
@CompoundIndex(def = "{'flowId':1, 'userId':1}", name = "index_flow_user", unique = true, sparse = true)
public class FlowUser {

    @Id
    private String id;

    @Indexed(name = "index_flow_id", sparse = true)
    private String flowId;

    @Indexed(name = "index_user_id", sparse = true)
    private String userId;

    private Date createdAt;

    private String createdBy;

    public FlowUser() {
        createdAt = Date.from(Instant.now());
    }

    public FlowUser(String userId) {
        this();
        this.userId = userId;
    }

    public FlowUser(String userId, String createdBy) {
        this();
        this.userId = userId;
        this.createdBy = createdBy;
    }
}
