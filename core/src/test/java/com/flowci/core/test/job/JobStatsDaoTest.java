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

package com.flowci.core.test.job;

import com.flowci.core.job.dao.JobStatsDao;
import com.flowci.core.job.domain.JobStats;
import com.flowci.core.test.SpringScenario;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class JobStatsDaoTest extends SpringScenario {

    @Autowired
    private JobStatsDao jobStatsDao;

    @Test
    public void should_save_with_string_type() {
        JobStats item = new JobStats();
        item.setType("JobStatus");
        item.setCreatedAt(new Date());
        item.setCreator("test@flow.ci");
        item.getData().put("STATUS", "PASSED");
        jobStatsDao.insert(item);

        item = jobStatsDao.findById(item.getId()).get();
        Assert.assertEquals("PASSED", item.getData().get("STATUS"));
    }

    @Test
    public void should_save_with_int_type() {
        JobStats item = new JobStats();
        item.setType("JUNIT");
        item.setCreatedAt(new Date());
        item.setCreator("test@flow.ci");
        item.getData().put("total", 10);
        item.getData().put("failures", 1);
        item.getData().put("errors", 4);
        item.getData().put("skipped", 2);
        jobStatsDao.insert(item);

        item = jobStatsDao.findById(item.getId()).get();
        Assert.assertEquals(10, item.getData().get("total"));
        Assert.assertEquals(1, item.getData().get("failures"));
        Assert.assertEquals(4, item.getData().get("errors"));
        Assert.assertEquals(2, item.getData().get("skipped"));
    }
}
