/*
 * Copyright 2020 flow.ci
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

package com.flowci.core.trigger.converter;

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.trigger.domain.GitTrigger;
import com.google.common.collect.ImmutableMap;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

public class GiteeConverter extends TriggerConverter  {

    public static final String Header = "X-Gitee-Event";

    public static final String HeaderForPing = "X-Gitee-Ping"; // X-Gitee-Ping: true

    public static final String PushEvent = "Push Hook";

    public static final String TagEvent = "Tag Push Hook";

    public static final String PR = "Merge Request Hook";

    @Override
    GitSource getGitSource() {
        return GitSource.GITEE;
    }

    @Override
    Map<String, Function<InputStream, GitTrigger>> getMapping() {
        return null;
    }
}
