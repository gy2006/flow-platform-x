/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.coverter.VariableMapReader;
import com.flowci.core.coverter.VariableMapWriter;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

/**
 * @author yang
 */
@Log4j2
@Configuration
@EnableMongoAuditing
public class MongoConfig extends AbstractMongoConfiguration {

    @Autowired
    private MongoProperties mongoProperties;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public MongoClient mongoClient() {
        log.info("Mongo URI: {}", mongoProperties.getUri());
        MongoClientURI uri = new MongoClientURI(mongoProperties.getUri());
        return new MongoClient(uri);
    }

    @Override
    protected String getDatabaseName() {
        return new MongoClientURI(mongoProperties.getUri()).getDatabase();
    }

    @Override
    public CustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new VariableMapReader(objectMapper));
        converters.add(new VariableMapWriter(objectMapper));
        return new MongoCustomConversions(converters);
    }
}
