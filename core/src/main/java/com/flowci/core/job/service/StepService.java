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

package com.flowci.core.job.service;

import com.flowci.core.job.domain.Job;
import com.flowci.domain.ExecutedCmd;
import com.flowci.tree.Node;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author yang
 */
public interface StepService {

    List<ExecutedCmd> init(Job job);

    /**
     * Get executed cmd for job and node
     */
    ExecutedCmd get(Job job, Node node);

    /**
     * List step of executed cmd for job
     */
    List<ExecutedCmd> list(Job job);

    /**
     * Get logs from agent
     */
    Page<String> logs(Job job, String executedCmdId, Pageable pageable);


    void update(Job job, ExecutedCmd cmd);

}
