/*
 * Copyright 2019 fir.im
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

package com.flowci.core.job.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.job.domain.Job.Status;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.trigger.domain.Variables;
import com.google.common.base.Strings;
import java.util.Date;
import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * As job list item to remove the data which are not used in the list view
 *
 * @author yang
 */
@Getter
@Setter
@Document(collection = "job")
public class JobItem {

    @Getter
    @Setter
    public static class Context extends HashMap<String, String> {

        public void putIfNotEmpty(String key, String value) {
            if (Strings.isNullOrEmpty(value)) {
                return;
            }

            this.put(key, value);
        }

    }

    public static class ContextReader implements Converter<org.bson.Document, Context> {

        @Override
        public Context convert(org.bson.Document source) {
            String branch = source.getString(Variables.GIT_BRANCH);
            String commitId = source.getString(Variables.GIT_COMMIT_ID);
            String commitMessage = source.getString(Variables.GIT_COMMIT_MESSAGE);

            Context itemContext = new Context();
            itemContext.putIfNotEmpty(Variables.GIT_BRANCH, branch);
            itemContext.putIfNotEmpty(Variables.GIT_COMMIT_ID, commitId);
            itemContext.putIfNotEmpty(Variables.GIT_COMMIT_MESSAGE, commitMessage);
            return itemContext;
        }
    }

    @Id
    protected String id;

    private Long buildNumber;

    @JsonIgnore
    private String flowId;

    private Trigger trigger;

    private Status status;

    private Context context;

    protected Date createdAt;
}