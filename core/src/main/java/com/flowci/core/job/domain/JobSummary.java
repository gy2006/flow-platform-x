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

package com.flowci.core.job.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * To describe job summary data
 */

@Setter
@Getter
@EqualsAndHashCode(of = "jobId")
@Accessors(chain = true)
@Document(collection = "job_summary")
@CompoundIndex(
        name = "index_job_summary_id_name_type",
        def = "{'jobId': 1, 'name': 1, 'type': 1}",
        unique = true
)
public class JobSummary {

    public enum Type {

        JSON,

        STRING
    }

    @Id
    private String id;

    @Indexed(name = "index_job_summary_jobid")
    private String jobId;

    private String name;

    private Type type;

    // base64 encoded data
    private String data;
}
