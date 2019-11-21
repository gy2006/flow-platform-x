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

package com.flowci.core.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.mongo.EncryptConverter;
import com.flowci.core.common.mongo.FlowMappingContext;
import com.flowci.core.common.mongo.VariableMapConverter;
import com.flowci.core.job.domain.JobItem;
import com.flowci.domain.ExecutedCmd;
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
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.util.ClassTypeInformation;

/**
 * @author yang
 */
@Log4j2
@Configuration
@EnableMongoAuditing
public class MongoConfig extends AbstractMongoConfiguration {

    @Autowired
    private ConfigProperties appProperties;

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

        VariableMapConverter variableConverter = new VariableMapConverter(objectMapper);
        converters.add(variableConverter.getReader());
        converters.add(variableConverter.getWriter());

        EncryptConverter encryptConverter = new EncryptConverter(appProperties.getSecret());
        converters.add(encryptConverter.new SimpleKeyPairReader());
        converters.add(encryptConverter.new SimpleKeyPairWriter());
        converters.add(encryptConverter.new SimpleAuthPairReader());
        converters.add(encryptConverter.new SimpleAuthPairWriter());

        converters.add(new JobItem.ContextReader());
        return new MongoCustomConversions(converters);
    }

    @Override
    public MongoMappingContext mongoMappingContext() throws ClassNotFoundException {
        FlowMappingContext mappingContext = new FlowMappingContext();
        mappingContext.setInitialEntitySet(getInitialEntitySet());
        mappingContext.setSimpleTypeHolder(customConversions().getSimpleTypeHolder());
        mappingContext.setFieldNamingStrategy(fieldNamingStrategy());

        mappingContext.addCustomizedPersistentEntity(ClassTypeInformation.from(ExecutedCmd.class), "executed_cmd");

        return mappingContext;
    }
}
