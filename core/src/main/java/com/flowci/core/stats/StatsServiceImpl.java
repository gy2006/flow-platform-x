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

package com.flowci.core.stats;

import com.flowci.core.common.helper.DateHelper;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.stats.dao.StatsItemDao;
import com.flowci.core.stats.domain.StatsCounter;
import com.flowci.core.stats.domain.StatsItem;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private StatsItemDao statsItemDao;

    @EventListener(JobStatusChangeEvent.class)
    public void onJobStatusChange(JobStatusChangeEvent event) {

    }

    @Override
    public StatsItem get(String flowId, String type, int day) {
        return statsItemDao.findByFlowIdAndDayAndType(flowId, day, type);
    }

    @Override
    public StatsItem add(Job job, String type, StatsCounter counter) {
        int day = DateHelper.toIntDay(job.getCreatedAt());
        StatsItem item = statsItemDao.findByFlowIdAndDayAndType(job.getFlowId(), day, type);

        if (Objects.isNull(item)) {
            item = new StatsItem()
                .setDay(day)
                .setFlowId(job.getFlowId())
                .setType(type)
                .setCounter(counter);
            return statsItemDao.insert(item);
        }

        item.getCounter().add(counter);
        return statsItemDao.save(item);
    }
}
