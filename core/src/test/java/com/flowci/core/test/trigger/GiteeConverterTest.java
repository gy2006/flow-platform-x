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

package com.flowci.core.test.trigger;

import com.flowci.core.test.SpringScenario;
import com.flowci.core.trigger.converter.GiteeConverter;
import com.flowci.core.trigger.converter.TriggerConverter;
import com.flowci.core.trigger.domain.GitPingTrigger;
import com.flowci.core.trigger.domain.GitTrigger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

public class GiteeConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter giteeConverter;

    @Test
    public void should_parse_ping_event() {
        InputStream stream = load("gitee/webhook_ping.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.Ping, stream);
        Assert.assertTrue(optional.isPresent());

        GitPingTrigger trigger = (GitPingTrigger) optional.get();
        Assert.assertNotNull(trigger);
    }
}
