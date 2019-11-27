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

package com.flowci.core.flow.service;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.StatsCounter;
import com.flowci.core.flow.domain.StatsItem;
import com.flowci.core.flow.domain.StatsType;
import java.util.List;
import java.util.Map;

/**
 * Statistic Service
 *
 * @author yang
 */
public interface StatsService {

    /**
     * List system default stats type
     */
    Map<String, StatsType> defaultTypes();

    /**
     * Get stats type from default and plugins
     */
    List<StatsType> getStatsType(Flow flow);

        /**
         * List statistic by range
         */
    List<StatsItem> list(String flowId, String type, int fromDay, int toDay);

    /**
     * Get statistic item
     */
    StatsItem get(String flowId, String type, int day);

    /**
     * Add statistic item
     */
    StatsItem add(String flowId, int day, String type, StatsCounter counter);

}
