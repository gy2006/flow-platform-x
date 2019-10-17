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

import com.flowci.core.api.domain.CreateJobSummary;
import com.flowci.core.common.helper.DateHelper;
import com.flowci.core.credential.dao.CredentialDao;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.flow.dao.FlowDao;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.dao.JobDao;
import com.flowci.core.job.dao.JobSummaryDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobSummary;
import com.flowci.core.job.util.JobKeyBuilder;
import com.flowci.core.stats.domain.StatsCounter;
import com.flowci.core.stats.domain.StatsItem;
import com.flowci.core.stats.service.StatsService;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.exception.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Log4j2
@Service
public class OpenRestServiceImpl implements OpenRestService {

    @Autowired
    private CredentialDao credentialDao;

    @Autowired
    private FlowDao flowDao;

    @Autowired
    private JobDao jobDao;

    @Autowired
    private JobSummaryDao jobSummaryDao;

    @Autowired
    private StatsService statsService;

    public Credential getCredential(String name, Class<? extends Credential> target) {
        Credential credential = credentialDao.findByName(name);

        if (Objects.isNull(credential)) {
            throw new NotFoundException("Credential {0} is not found", name);
        }

        if (credential.getClass().equals(target)) {
            return credential;
        }

        throw new NotFoundException("Credential {0} is not found", name);
    }

    @Override
    public StatsItem saveStatsForFlow(String flowName, String statsType, StatsCounter counter) {
        Flow flow = getFlow(flowName);
        int today = DateHelper.toIntDay(new Date());
        return statsService.add(flow.getId(), today, statsType, counter);
    }

    @Override
    public JobSummary saveJobSummary(String flowName, long buildNumber, CreateJobSummary body) {
        Flow flow = getFlow(flowName);
        String key = JobKeyBuilder.build(flow, buildNumber);
        Job job = jobDao.findByKey(key);

        if (Objects.isNull(job)) {
            throw new ArgumentException("Invalid job");
        }

        JobSummary summary = new JobSummary()
            .setJobId(job.getId())
            .setName(body.getName())
            .setType(JobSummary.Type.valueOf(body.getType()))
            .setData(body.getData());

        try {
            return jobSummaryDao.save(summary);
        } catch (DuplicateKeyException e) {
            log.warn("Duplicate job summary key");
            throw new DuplicateException("The job summary duplicated");
        }
    }

    private Flow getFlow(String name) {
        Flow flow = flowDao.findByName(name);
        if (Objects.isNull(flow)) {
            throw new ArgumentException("Invalid flow name");
        }
        return flow;
    }
}
