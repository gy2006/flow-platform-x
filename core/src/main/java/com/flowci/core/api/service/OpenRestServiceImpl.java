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
import com.flowci.core.credential.dao.CredentialDao;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.dao.FlowUserDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.StatsCounter;
import com.flowci.core.flow.service.StatsService;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobReportDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobReport;
import com.flowci.core.job.domain.JobReport.Type;
import com.flowci.core.job.util.JobKeyBuilder;
import com.flowci.core.user.dao.UserDao;
import com.flowci.core.user.domain.User;
import com.flowci.domain.ObjectWrapper;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import com.flowci.store.FileManager;
import com.flowci.store.Pathable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class OpenRestServiceImpl implements OpenRestService {

    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private FlowUserDao flowUserDao;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobReportDao jobReportDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private StatsService statsService;

    @Override
    public Credential getCredential(String name) {
        Optional<Credential> optional = credentialDao.findByName(name);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Credential {0} is not found", name);
    }

    @Override
    public void saveStatsForFlow(String flowName, String statsType, StatsCounter counter) {
        Flow flow = getFlow(flowName);
        int today = DateHelper.toIntDay(new Date());
        statsService.add(flow.getId(), today, statsType, counter);
    }

    @Override
    public void saveJobReport(String flowName, long buildNumber, CreateJobReport body) {
        Job job = getJob(flowName, buildNumber);
        Pathable flow = job::getFlowId;
        Pathable[] reportPath = {flow, job, JobReport.ReportPath};

        Type reportType = Type.valueOf(body.getType());
        ObjectWrapper<String> path = new ObjectWrapper<>();

        try (InputStream reportRaw = fromB64String(body.getData())) {
            path.setValue(fileManager.save(body.getName(), reportRaw, reportPath));

            jobReportDao.save(new JobReport()
                .setJobId(job.getId())
                .setName(body.getName())
                .setType(reportType)
                .setPath(path.getValue()));

        } catch (DuplicateKeyException e) {
            if (path.hasValue()) {
                try {
                    fileManager.remove(path.getValue());
                } catch (IOException ignore) {
                }
            }

            log.warn("Duplicate job summary key");
            throw new DuplicateException("The job summary duplicated");
        } catch (IOException e) {
            throw new ArgumentException("Invalid report data");
        }
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
        Flow flow = getFlow(flowName);
        List<String> userIds = flowUserDao.findAllUsers(flow.getId());
        return userDao.listUserEmailByIds(userIds);
    }

    private Job getJob(String name, long number) {
        Flow flow = getFlow(name);
        String key = JobKeyBuilder.build(flow, number);
        Optional<Job> optional = jobDao.findByKey(key);

        if (optional.isPresent()) {
            return optional.get();
        }

        throw new NotFoundException("Job for flow {0} with build number {1} not found", name, Long.toString(number));
    }

    private Flow getFlow(String name) {
        Flow flow = flowDao.findByName(name);
        if (Objects.isNull(flow)) {
            throw new ArgumentException("Invalid flow name");
        }
        return flow;
    }

    private static InputStream fromB64String(String val) {
        byte[] bytes = Base64.getDecoder().decode(val);
        return new ByteArrayInputStream(bytes);
    }
}
