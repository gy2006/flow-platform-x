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

import com.flowci.core.common.helper.DateHelper;
import com.flowci.core.job.domain.Job;
import com.flowci.core.stats.StatsService;
import com.flowci.core.stats.domain.StatsCounter;
import com.flowci.core.stats.domain.StatsItem;
import com.flowci.core.test.SpringScenario;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yang
 */
public class StatsServiceTest extends SpringScenario {

    @Autowired
    private StatsService statsService;

    @Test
    public void should_add_stats_item() {
        Job job = new Job();
        job.setFlowId("123-456");
        job.setCreatedAt(new Date());

        StatsCounter counter = new StatsCounter();
        counter.put("PASSED", 1.0F);
        counter.put("FAILED", 2.0F);

        String type = "JOB_STATUS";
        statsService.add(job, type, counter);
        statsService.add(job, type, counter);

        StatsItem item = statsService.get(job.getFlowId(), type, DateHelper.toIntDay(new Date()));
        Assert.assertNotNull(item);
        Assert.assertEquals(new Float(2.0F), item.getCounter().get("PASSED"));
        Assert.assertEquals(new Float(4.0F), item.getCounter().get("FAILED"));
    }
}
