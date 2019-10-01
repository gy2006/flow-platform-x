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

package com.flowci.core.trigger.converter;

import com.flowci.core.trigger.domain.GitTrigger;
import com.flowci.util.StringHelper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Log4j2
@Component("gitLabConverter")
public class GitLabConverter implements TriggerConverter {

    public static final String Header = "x-gitlab-event";

    public static final String Ping = "ping";

    public static final String Push = "Push Hook";

    public static final String Tag = "Tag Push Hook";

    public static final String PR = "Merge Request Hook";


    @Override
    public Optional<GitTrigger> convert(String event, InputStream body) {
        try {
            String s = StringHelper.toString(body);
            assert s != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
