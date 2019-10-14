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

package com.flowci.core.test.stats;

import com.flowci.core.job.domain.Job;
import com.flowci.core.stats.dao.StatsTypeDao;
import com.flowci.core.stats.domain.StatsItem;
import com.flowci.core.stats.domain.StatsType;
import com.flowci.core.test.SpringScenario;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class StatsTypeInitializerTest extends SpringScenario {

    @Autowired
    private StatsTypeDao statsTypeDao;

    @Test
    public void should_create_job_stats_type() {
        StatsType jobStatusType = statsTypeDao.findByName(StatsItem.TYPE_JOB_STATUS);
        Assert.assertNotNull(jobStatusType);

        List<String> fields = jobStatusType.getFields();
        Assert.assertTrue(fields.contains(Job.Status.SUCCESS.name()));
        Assert.assertTrue(fields.contains(Job.Status.FAILURE.name()));
        Assert.assertTrue(fields.contains(Job.Status.TIMEOUT.name()));
        Assert.assertTrue(fields.contains(Job.Status.CANCELLED.name()));
    }
}
