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

package com.flowci.core.stats.domain;

import com.flowci.core.job.domain.Job;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.LinkedList;
import java.util.List;

@Setter
@Getter
@EqualsAndHashCode(of = "name")
@Accessors(chain = true)
@Document(collection = "flow_stats_type")
public class StatsType {

    @Id
    private String id;

    @Indexed(name = "index_flow_stats_type_name", unique = true)
    private String name;

    // optional, reserved to create flow based stats
    private String flowId;

    // stats fields that applied in counter as key
    private List<String> fields = new LinkedList<>();

    public StatsItem createEmptyItem() {
        StatsCounter counter = new StatsCounter();
        for (String field : fields) {
            counter.put(field, 0.0F);
        }

        return new StatsItem()
                .setFlowId(flowId)
                .setType(name)
                .setCounter(counter);
    }
}
