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

import com.flowci.domain.VariableMap;
import com.flowci.util.StringHelper;
import com.google.common.base.Strings;
import java.io.Serializable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@Setter
public class GitPushTrigger extends GitTrigger {

    private Author author;

    private String commitId;

    private String message;

    private String ref;

    private String time;

    private String compareUrl;

    private String commitUrl;

    @Override
    public VariableMap toVariableMap() {
        VariableMap map = super.toVariableMap();

        map.putString(Variables.GIT_BRANCH, getBranchName());
        map.putString(Variables.GIT_COMPARE_URL, compareUrl);

        map.putString(Variables.GIT_COMMIT_ID, commitId);
        map.putString(Variables.GIT_COMMIT_MESSAGE, message);
        map.putString(Variables.GIT_COMMIT_TIME, time);
        map.putString(Variables.GIT_COMMIT_URL, commitUrl);
        map.putString(Variables.GIT_COMMIT_USER, author.email);
        return map;
    }

    private String getBranchName() {
        if (Strings.isNullOrEmpty(ref)) {
            return StringHelper.EMPTY;
        }

        int index = ref.lastIndexOf('/');
        if (index == -1) {
            return StringHelper.EMPTY;
        }

        return ref.substring(index + 1);
    }

    @Data
    public static class Author implements Serializable {

        private String name;

        private String username;

        private String email;
    }

    public static class Variables {

        public static final String GIT_BRANCH = "FLOWCI_GIT_BRANCH";

        public static final String GIT_COMPARE_URL = "FLOWCI_GIT_COMPARE_URL";

        public static final String GIT_COMMIT_ID = "FLOWCI_GIT_COMMIT_ID";

        public static final String GIT_COMMIT_MESSAGE = "FLOWCI_GIT_COMMIT_MESSAGE";

        public static final String GIT_COMMIT_TIME = "FLOWCI_GIT_COMMIT_TIME";

        public static final String GIT_COMMIT_URL = "FLOWCI_GIT_COMMIT_URL";

        public static final String GIT_COMMIT_USER = "FLOWCI_GIT_COMMIT_USER";

    }
}
