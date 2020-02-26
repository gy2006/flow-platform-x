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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.trigger.domain.*;
import com.flowci.core.trigger.util.BranchHelper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;

@Log4j2
@Component("giteeConverter")
public class GiteeConverter extends TriggerConverter  {

    public static final String Header = "X-Gitee-Event";

    public static final String HeaderForPing = "X-Gitee-Ping"; // X-Gitee-Ping: true

    public static final String Ping = "true";

    public static final String Push = "Push Hook";

    public static final String Tag = "Tag Push Hook";

    public static final String PR = "Merge Request Hook";

    private final Map<String, Function<InputStream, GitTrigger>> mapping =
            ImmutableMap.<String, Function<InputStream, GitTrigger>>builder()
                    .put(Ping, new EventConverter<>("Ping", GiteeConverter.PingEvent.class))
                    .put(Push, new EventConverter<>("Push", GiteeConverter.PushEvent.class))
                    .build();

    @Override
    GitSource getGitSource() {
        return GitSource.GITEE;
    }

    @Override
    Map<String, Function<InputStream, GitTrigger>> getMapping() {
        return mapping;
    }

    private static class PingEvent implements GitTriggerable {

        @Override
        public GitPingTrigger toTrigger() {
            GitPingTrigger trigger = new GitPingTrigger();
            trigger.setSource(GitSource.GITEE);
            trigger.setEvent(GitTrigger.GitEvent.PING);
            return trigger;
        }
    }

    private static class PushEvent implements GitTriggerable {

        public String ref;

        @JsonProperty("head_commit")
        public Commit commit;

        @JsonProperty("total_commits_count")
        public Integer numOfCommit;

        public Author pusher;

        @Override
        public GitTrigger toTrigger() {
            GitPushTrigger trigger = new GitPushTrigger();
            trigger.setSource(GitSource.GITEE);
            trigger.setEvent(GitTrigger.GitEvent.PUSH);
            trigger.setAuthor(pusher.toGitUser());
            trigger.setCommitId(commit.id);
            trigger.setMessage(commit.message);
            trigger.setCommitUrl(commit.url);
            trigger.setRef(BranchHelper.getBranchName(ref));
            trigger.setTime(commit.timestamp);
            trigger.setNumOfCommit(numOfCommit);
            return trigger;
        }
    }

    private static class Commit {

        public String id;

        public String message;

        public String timestamp;

        public String url;

        public Author author;
    }

    private static class Author {

        public String name;

        public String email;

        public String username;

        public GitUser toGitUser() {
            return new GitUser()
                    .setEmail(email)
                    .setName(name)
                    .setUsername(username);
        }
    }
}
