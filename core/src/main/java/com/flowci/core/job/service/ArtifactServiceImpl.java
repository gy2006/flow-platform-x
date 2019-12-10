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
import com.flowci.store.FileManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    }

    @Override
    public String fetch(Job job, String artifactId) {
        return null;
    }
}
