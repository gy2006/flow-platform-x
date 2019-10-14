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

package com.flowci.core.stats.dao;

import com.flowci.core.stats.domain.StatsItem;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author yang
 */
@Repository
public interface StatsItemDao extends MongoRepository<StatsItem, String> {

    @Query("{'flowId':?0, 'type': ?1, 'day' : {$gte : ?2, $lte : ?3}}")
    List<StatsItem> findByFlowIdAndTypeDayBetween(String flowId, String type, int dayGT, int dayLT, Sort sort);

    @Query("{'flowId':?0, 'day' : {$gte : ?1, $lte : ?2}}")
    List<StatsItem> findByFlowIdDayBetween(String flowId, int dayGT, int dayLT, Sort sort);

    StatsItem findByFlowIdAndDayAndType(String flowId, int day, String type);
}
