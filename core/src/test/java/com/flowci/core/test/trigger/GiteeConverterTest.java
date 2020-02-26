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

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.trigger.converter.GiteeConverter;
import com.flowci.core.trigger.converter.TriggerConverter;
import com.flowci.core.trigger.domain.GitPingTrigger;
import com.flowci.core.trigger.domain.GitPushTrigger;
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

    @Test
    public void should_parse_push_event() {
        InputStream stream = load("gitee/webhook_push.json");

        Optional<GitTrigger> optional = giteeConverter.convert(GiteeConverter.Push, stream);
        Assert.assertTrue(optional.isPresent());

        GitPushTrigger trigger = (GitPushTrigger) optional.get();
        Assert.assertNotNull(trigger);

        Assert.assertEquals(GitSource.GITEE, trigger.getSource());
        Assert.assertEquals("feature/222", trigger.getRef());
        Assert.assertEquals("ea926aebbe8738e903345534a9b158716b904816", trigger.getCommitId());
        Assert.assertEquals("update README.md.\ntest pr message..", trigger.getMessage());
        Assert.assertEquals("https://gitee.com/gy2006/flow-test/commit/ea926aebbe8738e903345534a9b158716b904816", trigger.getCommitUrl());
        Assert.assertEquals("2020-02-25T22:52:24+08:00", trigger.getTime());
        Assert.assertEquals(1, trigger.getNumOfCommit());

        Assert.assertEquals("gy2006", trigger.getAuthor().getUsername());
        Assert.assertEquals("benqyang_2006@hotmail.com", trigger.getAuthor().getEmail());
        Assert.assertEquals("yang.guo", trigger.getAuthor().getName());
    }
}
