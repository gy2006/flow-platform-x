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

package com.flowci.core.test.common;

import com.flowci.core.common.manager.FileManager;
import com.flowci.core.common.manager.MinioFileManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.domain.Job;
import com.flowci.core.test.SpringScenario;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class MinioFileManagerTest extends SpringScenario {

    @Autowired
    private FileManager fileManager;

    @Test
    public void should_be_minio_instance() {
        Assert.assertTrue(fileManager instanceof MinioFileManager);
    }

    @Test
    public void should_create_bucket_for_flow() throws IOException {
        Flow flow = new Flow();
        flow.setId("flowid");

        Job job = new Job();
        job.setBuildNumber(10L);

        String key = fileManager.create(flow, job);
        Assert.assertNotNull(key);
        Assert.assertEquals("flowid/10/", key);
    }
}
