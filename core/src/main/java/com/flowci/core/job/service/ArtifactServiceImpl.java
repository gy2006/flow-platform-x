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

import com.flowci.core.job.dao.JobArtifactDao;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.JobArtifact;
import com.flowci.exception.NotAvailableException;
import com.flowci.exception.NotFoundException;
import com.flowci.store.FileManager;
import com.flowci.store.Pathable;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class ArtifactServiceImpl implements ArtifactService {

    @Autowired
    private JobArtifactDao jobArtifactDao;

    @Qualifier("fileManager")
    @Autowired
    private FileManager fileManager;

    @Override
    public List<JobArtifact> list(Job job) {
        return jobArtifactDao.findAllByJobId(job.getId());
    }

    @Override
    public void save(Job job, MultipartFile file) {
        Pathable[] reportPath = getReportPath(job);

        try (InputStream reportRaw = file.getInputStream()) {
            // save to file manager by file name
            String path = fileManager.save(file.getOriginalFilename(), reportRaw, reportPath);

            JobArtifact artifact = new JobArtifact();
            artifact.setJobId(job.getId());
            artifact.setFileName(file.getOriginalFilename());
            artifact.setContentType(file.getContentType());
            artifact.setContentSize(file.getSize());
            artifact.setPath(path);
            artifact.setCreatedAt(new Date());

            jobArtifactDao.save(artifact);
        } catch (IOException e) {
            throw new NotAvailableException("Invalid artifact data");
        }
    }

    @Override
    public JobArtifact fetch(Job job, String artifactId) {
        Optional<JobArtifact> optional = jobArtifactDao.findById(artifactId);
        if (!optional.isPresent()) {
            throw new NotFoundException("The job artifact not available");
        }

        try {
            JobArtifact artifact = optional.get();
            InputStream stream = fileManager.read(artifact.getFileName(), getReportPath(job));
            artifact.setSrc(stream);
            return artifact;
        } catch (IOException e) {
            throw new NotAvailableException("Invalid job artifact");
        }
    }

    private static Pathable[] getReportPath(Job job) {
        Pathable flow = job::getFlowId;
        return new Pathable[]{flow, job, JobArtifact.ArtifactPath};
    }
}
