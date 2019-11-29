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

import com.flowci.core.common.domain.Pathable;
import com.flowci.core.common.manager.FileManager;
import com.flowci.core.common.manager.MinioFileManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.domain.Job;
import com.flowci.core.test.SpringScenario;
import com.flowci.util.StringHelper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;

public class MinioFileManagerTest extends SpringScenario {

    private final Flow flow = new Flow();

    private final Job job = new Job();

    {
        flow.setId("flowid");
        job.setBuildNumber(10L);
    }

    @Autowired
    private FileManager fileManager;

    @Test
    public void should_be_minio_instance() {
        Assert.assertTrue(fileManager instanceof MinioFileManager);
    }

    @Test
    public void should_save_and_read_object() throws IOException {
        final String fileName = "test.log";
        final String content = "my-test-log";
        final Pathable[] dir = {flow, job, FileManager.LogPath};

        // when: save the file
        InputStream data = StringHelper.toInputStream(content);
        String logPath = fileManager.save(fileName, data, dir);
        Assert.assertNotNull(logPath);
        Assert.assertEquals("flowid/10/logs/test.log", logPath);

        // then: content should be read
        boolean exist = fileManager.exist(fileName, dir);
        Assert.assertTrue(exist);

        InputStream read = fileManager.read(fileName, dir);
        Assert.assertEquals(content, StringHelper.toString(read));

        // when: delete
        fileManager.remove(fileName, dir);

        // then: should throw IOException since not existed
        exist = fileManager.exist(fileName, dir);
        Assert.assertFalse(exist);
    }

    @Test(expected = IOException.class)
    public void should_throw_exception_if_not_found() throws IOException {
        fileManager.read("hello", flow, job, FileManager.LogPath);
    }
}
