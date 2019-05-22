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

package com.flowci.core.job;

import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.job.domain.CreateJob;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.domain.JobYml;
import com.flowci.core.job.service.JobService;
import com.flowci.core.job.service.LoggingService;
import com.flowci.core.job.service.StepService;
import com.flowci.domain.ExecutedCmd;
import com.flowci.domain.VariableMap;
import com.flowci.exception.ArgumentException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/jobs")
public class JobController {

    private static final String DefaultPage = "0";

    private static final String DefaultSize = "20";

    private static final String ParameterLatest = "latest";

    @Autowired
    private FlowService flowService;

    @Autowired
    private JobService jobService;

    @Autowired
    private StepService stepService;

    @Autowired
    private LoggingService loggingService;

    @GetMapping("/{flow}")
    public Page<Job> list(@PathVariable("flow") String name,
                          @RequestParam(required = false, defaultValue = DefaultPage) int page,
                          @RequestParam(required = false, defaultValue = DefaultSize) int size) {

        Flow flow = flowService.get(name);
        return jobService.list(flow, page, size);
    }

    @GetMapping("/{flow}/{buildNumberOrLatest}")
    public Job get(@PathVariable("flow") String name, @PathVariable String buildNumberOrLatest) {
        Flow flow = flowService.get(name);

        if (ParameterLatest.equals(buildNumberOrLatest)) {
            return jobService.getLatest(flow);
        }

        try {
            long buildNumber = Long.parseLong(buildNumberOrLatest);
            return jobService.get(flow, buildNumber);
        } catch (NumberFormatException e) {
            throw new ArgumentException("Build number must be a integer");
        }
    }

    @GetMapping(value = "/{flow}/{buildNumber}/yml", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getYml(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        JobYml yml = jobService.getYml(job);
        return yml.getRaw();
    }

    @GetMapping("/{flow}/{buildNumberOrLatest}/steps")
    public List<ExecutedCmd> getSteps(@PathVariable String flow,
                                      @PathVariable String buildNumberOrLatest) {
        Job job = get(flow, buildNumberOrLatest);
        return stepService.list(job);
    }

    @GetMapping("/logs/{executedCmdId}")
    public Page<String> getStepLog(@PathVariable String executedCmdId,
                                   @RequestParam(required = false, defaultValue = "0") int page,
                                   @RequestParam(required = false, defaultValue = "50") int size) {

        return loggingService.read(stepService.get(executedCmdId), PageRequest.of(page, size));
    }

    @PostMapping
    public Job create(@Validated @RequestBody CreateJob data) {
        Flow flow = flowService.get(data.getFlow());
        Yml yml = flowService.getYml(flow);
        return jobService.create(flow, yml, Trigger.API, VariableMap.EMPTY);
    }

    @PostMapping("/run")
    public Job createAndRun(@Validated @RequestBody CreateJob data) {
        Job job = create(data);
        return jobService.start(job);
    }

    @PostMapping("/{flow}/{buildNumber}/cancel")
    public Job cancel(@PathVariable String flow, @PathVariable String buildNumber) {
        Job job = get(flow, buildNumber);
        return jobService.cancel(job);
    }
}
