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

import com.flowci.core.stats.domain.StatsItem;
import com.flowci.core.stats.domain.StatsType;
import com.flowci.core.stats.service.StatsService;
import com.flowci.exception.ArgumentException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@RestController
@RequestMapping("/stats")
public class StatsController {

    private static final int MaxDays = 30;

    @Autowired
    private StatsService statsService;

    @GetMapping("/type/{name}")
    public StatsType getMetaType(@PathVariable String name) {
        return statsService.getMetaType(name);
    }

    @GetMapping
    public List<StatsItem> list(@RequestParam String id,
                                @RequestParam(required = false) String t,
                                @RequestParam int from,
                                @RequestParam int to) {

        if (to < from || to - from > MaxDays) {
            throw new ArgumentException("Illegal query argument");
        }

        return statsService.list(id, t, from, to);
    }

}
