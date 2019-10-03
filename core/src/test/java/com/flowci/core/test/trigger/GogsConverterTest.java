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

package com.flowci.core.test.trigger;

import com.flowci.core.test.SpringScenario;
import com.flowci.core.trigger.converter.GitHubConverter;
import com.flowci.core.trigger.converter.GogsConverter;
import com.flowci.core.trigger.converter.TriggerConverter;
import com.flowci.core.trigger.domain.GitPushTrigger;
import com.flowci.core.trigger.domain.GitTrigger;
import com.flowci.core.trigger.domain.GitUser;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.InputStream;
import java.util.Optional;

public class GogsConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter gogsConverter;

    @Test
    public void should_get_push_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_push.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.Push, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPushTrigger);

        GitPushTrigger trigger = (GitPushTrigger) optional.get();
        Assert.assertEquals("master", trigger.getRef());
        Assert.assertEquals("62f02963619d8fa1a03afb65ad3ed6b8d3c0fd69", trigger.getCommitId());
        Assert.assertEquals("Update 'README.md'\n\nhello\n", trigger.getMessage());
        Assert.assertEquals(
                "http://localhost:3000/test/my-first-repo/commit/62f02963619d8fa1a03afb65ad3ed6b8d3c0fd69",
                trigger.getCommitUrl());
        Assert.assertEquals("2019-10-03T10:44:15Z", trigger.getTime());

        GitUser pusher = trigger.getAuthor();
        Assert.assertEquals("benqyang_2006@gogs.test", pusher.getEmail());
        Assert.assertEquals("test", pusher.getUsername());
        Assert.assertEquals(
                "https://secure.gravatar.com/avatar/0dce14d99e8295e36aca078f195fa0c3?d=identicon",
                pusher.getAvatarLink());
    }

    @Test
    public void should_get_tag_trigger_from_gogs_event() {
        InputStream stream = load("gogs/webhook_tag.json");

        Optional<GitTrigger> optional = gogsConverter.convert(GogsConverter.Tag, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPushTrigger);

        GitPushTrigger tag = (GitPushTrigger) optional.get();
        Assert.assertEquals("v4.0", tag.getRef());
        Assert.assertEquals("4", tag.getCommitId());
        Assert.assertEquals("2019-10-03T12:46:57Z", tag.getTime());
        Assert.assertEquals("title for v4.0", tag.getMessage());
        Assert.assertEquals("", tag.getCommitUrl());

        GitUser author = tag.getAuthor();
        Assert.assertEquals("test", author.getUsername());
        Assert.assertEquals("benqyang_2006@gogs.com", author.getEmail());
        Assert.assertEquals(
                "https://secure.gravatar.com/avatar/0dce14d99e8295e36aca078f195fa0c3?d=identicon",
                author.getAvatarLink());
    }
}
