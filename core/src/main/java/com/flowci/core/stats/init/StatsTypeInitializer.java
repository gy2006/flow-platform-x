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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.stats.dao.StatsTypeDao;
import com.flowci.core.stats.domain.StatsType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

/**
 * Init statistic type from json 'default_statistic_type.json'
 */

@Log4j2
@Component
public final class StatsTypeInitializer {

    private final static TypeReference<List<StatsType>> StatsTypeRef = new TypeReference<List<StatsType>>() {
    };

    @Value("classpath:default_statistic_type.json")
    private Resource defaultTypeJsonFile;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StatsTypeDao statsTypeDao;

    @PostConstruct
    public void initJobStatsType() {
        try {
            List<StatsType> types = objectMapper.readValue(defaultTypeJsonFile.getInputStream(), StatsTypeRef);

            for (StatsType t : types) {
                save(t);
            }
        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }

    private void save(StatsType t) {
        try {
            statsTypeDao.save(t);
        } catch (DuplicateKeyException ignore) {

        }
    }
}
