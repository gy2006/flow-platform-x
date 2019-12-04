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

package com.flowci.core.job.service;

import com.flowci.core.job.dao.JobReportDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobReport;
import com.flowci.domain.ObjectWrapper;
import com.flowci.exception.ArgumentException;
import com.flowci.exception.DuplicateException;
import com.flowci.store.FileManager;
import com.flowci.store.Pathable;
import com.google.common.collect.Sets;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Log4j2
@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private JobReportDao jobReportDao;

    @Autowired
    private FileManager fileManager;

    @Override
    public List<JobReport> list(Job job) {
        return jobReportDao.findAllByJobId(job.getId());
    }

    @Override
    public void save(String name, Set<String> types, Job job, MultipartFile file) {
        Pathable flow = job::getFlowId;
        Pathable[] reportPath = {flow, job, JobReport.ReportPath};
        ObjectWrapper<String> path = new ObjectWrapper<>();

        try (InputStream reportRaw = file.getInputStream()) {

            // save to file manager by unique report name
            path.setValue(fileManager.save(name, reportRaw, reportPath));

            // save to job report db
            JobReport r = new JobReport();
            r.setName(name);
            r.setJobId(job.getId());
            r.setFileName(file.getOriginalFilename());
            r.setContentType(types);
            r.setContentSize(file.getSize());
            r.setPath(path.getValue());
            r.setCreatedAt(new Date());

            jobReportDao.save(r);

        } catch (DuplicateKeyException e) {
            if (path.hasValue()) {
                try {
                    fileManager.remove(path.getValue());
                } catch (IOException ignore) {
                }
            }

            log.warn("The job report duplicated");
            throw new DuplicateException("The job report duplicated");
        } catch (IOException e) {
            throw new ArgumentException("Invalid report data");
        }
    }
}
