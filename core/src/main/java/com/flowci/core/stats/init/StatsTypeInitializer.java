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

package com.flowci.core.stats.init;

import com.flowci.core.job.domain.Job;
import com.flowci.core.stats.dao.StatsTypeDao;
import com.flowci.core.stats.domain.StatsItem;
import com.flowci.core.stats.domain.StatsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public final class StatsTypeInitializer {

    @Autowired
    private StatsTypeDao statsTypeDao;

    @PostConstruct
    public void initJobStatsType() {
        StatsType jobStatusType = new StatsType().setName(StatsItem.TYPE_JOB_STATUS);
        for (Job.Status status : Job.FINISH_STATUS) {
            jobStatusType.getFields().add(status.name());
        }

        try {
            statsTypeDao.save(jobStatusType);
        } catch (DuplicateKeyException ignore) {

        }
    }
}
