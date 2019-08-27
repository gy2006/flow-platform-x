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

package com.flowci.core.test.common;

import com.flowci.core.common.manager.PathManager;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.job.domain.Job;
import com.flowci.core.test.SpringScenario;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class PathManagerTest extends SpringScenario {

    @Autowired
    private PathManager pathManager;

    @Test
    public void should_operate_dir_by_pathable_objs() throws IOException {
        Flow flow = new Flow();
        flow.setId("flow");

        Job job = new Job();
        job.setId("job");

        pathManager.create(flow, job);
        Assert.assertTrue(pathManager.exist(flow, job));

        pathManager.delete(flow, job);
        Assert.assertFalse(pathManager.exist(flow, job));
    }
}
