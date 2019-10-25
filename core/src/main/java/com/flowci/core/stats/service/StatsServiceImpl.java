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

package com.flowci.core.stats.service;

import com.flowci.core.common.helper.DateHelper;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.event.JobStatusChangeEvent;
import com.flowci.core.stats.dao.StatsItemDao;
import com.flowci.core.stats.dao.StatsTypeDao;
import com.flowci.core.stats.domain.StatsCounter;
import com.flowci.core.stats.domain.StatsItem;

import com.flowci.exception.NotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.flowci.core.stats.domain.StatsType;
import com.flowci.util.StringHelper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Log4j2
@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private StatsTypeDao statsTypeDao;

    @Autowired
    private StatsItemDao statsItemDao;

    @EventListener(JobStatusChangeEvent.class)
    public void onJobStatusChange(JobStatusChangeEvent event) {
        Job job = event.getJob();

        if (!job.isDone()) {
            return;
        }

        Optional<StatsType> optional = statsTypeDao.findByName(StatsItem.TYPE_JOB_STATUS);
        if (!optional.isPresent()) {
            log.warn("Job statistic type is missing");
            return;
        }

        StatsItem item = optional.get().createEmptyItem();
        item.getCounter().put(job.getStatus().name(), 1.0F);

        int day = DateHelper.toIntDay(job.getCreatedAt());
        add(job.getFlowId(), day, item.getType(), item.getCounter());
    }

    @Override
    public List<StatsItem> list(String flowId, String type, int fromDay, int toDay) {
        Sort sort = new Sort(Sort.Direction.ASC, "day");

        if (StringHelper.hasValue(type)) {
            return statsItemDao.findByFlowIdAndTypeDayBetween(flowId, type, fromDay, toDay, sort);
        }

        return statsItemDao.findByFlowIdDayBetween(flowId, fromDay, toDay, sort);
    }

    @Override
    public StatsType getMetaType(String name) {
        Optional<StatsType> optional = statsTypeDao.findByName(name);
        if (!optional.isPresent()) {
            throw new NotFoundException("Stats type {0} is not found", name);
        }
        return optional.get();
    }

    @Override
    public List<StatsType> getMetaTypeList() {
        return statsTypeDao.findAll();
    }

    @Override
    public StatsItem get(String flowId, String type, int day) {
        return statsItemDao.findByFlowIdAndDayAndType(flowId, day, type);
    }

    @Override
    public StatsItem add(String flowId, int day, String type, StatsCounter counter) {
        StatsItem item = statsItemDao.findByFlowIdAndDayAndType(flowId, day, type);

        if (Objects.isNull(item)) {
            item = new StatsItem()
                    .setDay(day)
                    .setFlowId(flowId)
                    .setType(type)
                    .setCounter(counter);
            return statsItemDao.insert(item);
        }

        item.getCounter().add(counter);
        return statsItemDao.save(item);
    }
}
