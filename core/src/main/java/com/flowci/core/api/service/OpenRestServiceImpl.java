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

package com.flowci.core.api.service;

import com.flowci.core.api.domain.CreateJobReport;
import com.flowci.core.common.helper.DateHelper;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.credential.service.CredentialService;
import com.flowci.core.flow.dao.FlowUserDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.StatsCounter;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.StatsService;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.service.ArtifactService;
import com.flowci.core.job.service.ReportService;
import com.flowci.core.job.util.JobKeyBuilder;
import com.flowci.core.user.dao.UserDao;
import com.flowci.core.user.domain.User;
import com.flowci.exception.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
public class OpenRestServiceImpl implements OpenRestService {

    @Autowired
    private FlowUserDao flowUserDao;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private FlowService flowService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private StatsService statsService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private ArtifactService artifactService;

    @Override
    public Credential getCredential(String name) {
        return credentialService.get(name);
    }

    @Override
    public void saveStatsForFlow(String flowName, String statsType, StatsCounter counter) {
        Flow flow = flowService.get(flowName);
        int today = DateHelper.toIntDay(new Date());
        statsService.add(flow.getId(), today, statsType, counter);
    }

    @Override
    public void saveJobReport(String flowName, long buildNumber, CreateJobReport report, MultipartFile file) {
        Job job = getJob(flowName, buildNumber);
        reportService.save(report.getName(), report.getType(), report.getZipped(), report.getEntryFile(), job, file);
    }

    @Override
    public void saveJobArtifact(String flowName, long buildNumber, MultipartFile file) {
        Job job = getJob(flowName, buildNumber);
        artifactService.save(job, file);
    }

    @Override
    public void addToJobContext(String flowName, long buildNumber, Map<String, String> vars) {
        Job job = getJob(flowName, buildNumber);

        // TODO: verify key value string

        job.getContext().putAll(vars);
        jobDao.save(job);
    }

    @Override
    public List<User> users(String flowName) {
        Flow flow = flowService.get(flowName);
        List<String> userIds = flowUserDao.findAllUsers(flow.getId());
        return userDao.listUserEmailByIds(userIds);
    }

    private Job getJob(String name, long number) {
        Flow flow = flowService.get(name);
        String key = JobKeyBuilder.build(flow, number);
        Optional<Job> optional = jobDao.findByKey(key);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Job for flow {0} with build number {1} not found", name, Long.toString(number));
    }
}
