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

import com.flowci.core.common.domain.GitSource;
import com.flowci.core.test.SpringScenario;
import com.flowci.core.trigger.converter.GitLabConverter;
import com.flowci.core.trigger.converter.TriggerConverter;
import com.flowci.core.trigger.domain.GitPrTrigger;
import com.flowci.core.trigger.domain.GitPushTrigger;
import com.flowci.core.trigger.domain.GitTrigger;
import com.flowci.core.trigger.domain.GitTrigger.GitEvent;
import com.flowci.core.trigger.domain.GitUser;
import java.io.InputStream;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class GitLabConverterTest extends SpringScenario {

    @Autowired
    private TriggerConverter gitLabConverter;

    @Test
    public void should_get_push_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_push.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.Push, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPushTrigger);

        GitPushTrigger push = (GitPushTrigger) optional.get();
        Assert.assertEquals(GitTrigger.GitEvent.PUSH, push.getEvent());
        Assert.assertEquals(GitSource.GITLAB, push.getSource());

        Assert.assertEquals("d8e7334543d437c1a889a9187e66d1968280d7d4", push.getCommitId());
        Assert.assertEquals("master", push.getRef());
        Assert.assertEquals("Update .flow.yml test", push.getMessage());
        Assert.assertEquals("2017-10-17T08:23:36Z", push.getTime());
        Assert.assertEquals(
            "https://gitlab.com/yang-guo-2016/kai-web/commit/d8e7334543d437c1a889a9187e66d1968280d7d4",
            push.getCommitUrl());
        Assert.assertEquals(3, push.getNumOfCommit());

        GitUser author = push.getAuthor();
        Assert.assertEquals("yang-guo-2016", author.getUsername());
        Assert.assertEquals("benqyang_2006@hotmail.com", author.getEmail());
        Assert.assertEquals(
            "https://secure.gravatar.com/avatar/25fc63da4f632d2a2c10724cba3b9efc?s=80\u0026d=identicon",
            author.getAvatarLink());
    }

    @Test
    public void should_get_tag_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_tag.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.Tag, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPushTrigger);

        GitPushTrigger tag = (GitPushTrigger) optional.get();
        Assert.assertEquals(GitEvent.TAG, tag.getEvent());
        Assert.assertEquals(GitSource.GITLAB, tag.getSource());

        Assert.assertEquals("ee31197fd0fab68d1e5ab56dabcfae150ab5d057", tag.getCommitId());
        Assert.assertEquals("v2.0", tag.getRef());
        Assert.assertEquals("test tag push", tag.getMessage());
        Assert.assertEquals(1, tag.getNumOfCommit());

        GitUser author = tag.getAuthor();
        Assert.assertEquals("yang-guo-2016", author.getUsername());
        Assert.assertEquals("gy@fir.im", author.getEmail());
        Assert.assertEquals(
            "https://secure.gravatar.com/avatar/25fc63da4f632d2a2c10724cba3b9efc?s=80\u0026d=identicon",
            author.getAvatarLink());
    }

    @Test
    public void should_get_pr_open_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_mr_opened.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.PR, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger pr = (GitPrTrigger) optional.get();
        Assert.assertEquals(GitTrigger.GitEvent.PR_OPENED, pr.getEvent());
        Assert.assertEquals(GitSource.GITLAB, pr.getSource());
        Assert.assertFalse(pr.getMerged());
        Assert.assertEquals("Update package.json title", pr.getTitle());
        Assert.assertEquals("pr message", pr.getBody());
        Assert.assertEquals("2017-08-08T08:44:54.622Z", pr.getTime());
        Assert.assertEquals("https://gitlab.com/yang-guo-2016/kai-web/merge_requests/1", pr.getUrl());

        GitPrTrigger.Source from = pr.getHead();
        Assert.assertEquals("kai-web", from.getRepoName());
        Assert.assertEquals("https://gitlab.com/yang-guo-2016/kai-web", from.getRepoUrl());
        Assert.assertEquals("developer", from.getRef());
        Assert.assertEquals("9e81037427cc1c50641c5ffc7b6c70a487886ed8", from.getCommit());

        GitPrTrigger.Source to = pr.getBase();
        Assert.assertEquals("kai-web", to.getRepoName());
        Assert.assertEquals("https://gitlab.com/yang-guo-2016/kai-web", to.getRepoUrl());
        Assert.assertEquals("master", to.getRef());
        Assert.assertEquals("", to.getCommit());

        GitUser sender = pr.getSender();
        Assert.assertNull(sender.getEmail());
        Assert.assertEquals("yang-guo-2016", sender.getUsername());
        Assert.assertEquals(
            "https://secure.gravatar.com/avatar/25fc63da4f632d2a2c10724cba3b9efc?s=80\u0026d=identicon",
            sender.getAvatarLink());
    }

    @Test
    public void should_get_pr_close_trigger_from_gitlab_event() {
        InputStream stream = load("gitlab/webhook_mr_merged.json");

        Optional<GitTrigger> optional = gitLabConverter.convert(GitLabConverter.PR, stream);
        Assert.assertTrue(optional.isPresent());
        Assert.assertTrue(optional.get() instanceof GitPrTrigger);

        GitPrTrigger pr = (GitPrTrigger) optional.get();
        Assert.assertEquals(GitEvent.PR_MERGED, pr.getEvent());
        Assert.assertEquals(GitSource.GITLAB, pr.getSource());
        Assert.assertTrue(pr.getMerged());
    }
}
