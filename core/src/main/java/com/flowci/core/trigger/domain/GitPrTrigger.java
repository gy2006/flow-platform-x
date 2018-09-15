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

package com.flowci.core.trigger.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@Setter
public class GitPrTrigger extends GitTrigger {

    private String title;

    private String body;

    private String url;

    private String number;

    private String time;

    private String numOfCommits;

    private String numOfFileChanges;

    private Sender sender;

    private Boolean merged;

    private Source head;

    private Source base;


    @Getter
    @Setter
    public static class Sender {

        private String id;

        private String username;
    }

    @Getter
    @Setter
    public static class Source {

        private String ref;

        private String commit;

        private String repoName;

        private String repoUrl;
    }
}
