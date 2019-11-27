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

package com.flowci.core.flow.controller;

import com.flowci.core.common.helper.DateHelper;
import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.StatsItem;
import com.flowci.core.flow.domain.StatsType;
import com.flowci.core.flow.service.FlowService;
import com.flowci.core.flow.service.StatsService;
import com.flowci.exception.ArgumentException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author yang
 */
@RestController
@RequestMapping("/flows")
public class StatsController {

    private static final int MaxDays = 30;

    @Autowired
    private FlowService flowService;

    @Autowired
    private StatsService statsService;

    @GetMapping("/{name}/stats/types")
    public List<StatsType> types(@PathVariable String name) {
        Flow flow = flowService.get(name);
        return statsService.getStatsType(flow);
    }

    @GetMapping("/{name}/stats/total")
    public StatsItem total(@PathVariable String name, @RequestParam String t) {
        Flow flow = flowService.get(name);
        return statsService.get(flow.getId(), t, StatsItem.ZERO_DAY);
    }

    @GetMapping("/{name}/stats")
    public List<StatsItem> list(@PathVariable String name,
                                @RequestParam(required = false) String t,
                                @RequestParam int from,
                                @RequestParam int to) {

        if (isValidDuration(from, to)) {
            throw new ArgumentException("Illegal query argument");
        }

        Flow flow = flowService.get(name);
        return statsService.list(flow.getId(), t, from, to);
    }

    private boolean isValidDuration(int from, int to) {
        Instant f = DateHelper.toInstant(from);
        Instant t = DateHelper.toInstant(to);

        if (f.isAfter(t)) {
            return false;
        }

        if (f.plus(MaxDays, ChronoUnit.DAYS).isAfter(t)) {
            return false;
        }

        return true;
    }

}
