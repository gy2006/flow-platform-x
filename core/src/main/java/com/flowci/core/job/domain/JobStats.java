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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author yang
 */
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Document(collection = "job_statistic")
public class JobStats {

    @Id
    private String id; // job id

    private String flowId;

    /**
     * Status Type, ex: status, ut, code coverage
     */
    private String type;

    private String branch;

    /**
     * User email from git if trigger from git
     * Or email of system if trigger from ui
     * Or NULL if trigger from cron
     */
    private String creator;

    private Date createdAt;

    private Map<String, Object> data = new HashMap<>();
}
