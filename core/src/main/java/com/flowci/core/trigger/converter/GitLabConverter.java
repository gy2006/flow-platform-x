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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.trigger.domain.GitPushTrigger;
import com.flowci.core.trigger.domain.GitTrigger;
import com.flowci.core.trigger.domain.GitUser;
import com.flowci.core.trigger.util.BranchHelper;
import com.flowci.exception.ArgumentException;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

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

    private final Map<String, Function<InputStream, Optional<GitTrigger>>> mapping =
            ImmutableMap.<String, Function<InputStream, Optional<GitTrigger>>>builder()
                    .put(Push, new PushEventConverter())
                    .build();

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Optional<GitTrigger> convert(String event, InputStream body) {
        return mapping.get(event).apply(body);
    }

    // ======================================================
    //      Converters
    // ======================================================

    private class PushEventConverter implements Function<InputStream, Optional<GitTrigger>> {

        @Override
        public Optional<GitTrigger> apply(InputStream stream) {
            try {
                PushEvent event = objectMapper.readValue(stream, PushEvent.class);
                return event.toTrigger();
            } catch (IOException e) {
                log.warn("Unable to parse Gitlab push event");
                return Optional.empty();
            }
        }
    }

    // ======================================================
    //      Objects for GitLab
    // ======================================================

    private abstract static class Event {

        @JsonProperty("event_name")
        private String name;

        abstract Optional<GitTrigger> toTrigger();
    }

    private static class PushEvent extends Event {

        public String before;

        public String after;

        public String ref;

        @JsonAlias("user_id")
        public String userId;

        @JsonAlias("user_name")
        public String nameOfUser;

        @JsonAlias("user_username")
        public String username;

        @JsonAlias("user_email")
        public String email;

        @JsonAlias("user_avatar")
        public String avatar;

        public List<Commit> commits;

        @Override
        Optional<GitTrigger> toTrigger() {
            GitPushTrigger trigger = new GitPushTrigger();
            trigger.setSource(GitSource.GITLAB);
            trigger.setEvent(GitTrigger.GitEvent.PUSH);

            if (commits == null || commits.size() == 0) {
                throw new ArgumentException("No commits data on GitLab push event");
            }

            Commit topCommit = commits.get(0);

            trigger.setCommitId(topCommit.id);
            trigger.setMessage(topCommit.message);
            trigger.setCommitUrl(topCommit.url);
            trigger.setCompareUrl(topCommit.url);
            trigger.setRef(BranchHelper.getBranchName(ref));
            trigger.setTime(topCommit.timestamp);

            // set commit author info
            GitUser gitUser = new GitUser()
                    .setUsername(topCommit.author.name)
                    .setEmail(topCommit.author.email);

            if (Objects.equals(topCommit.author.name, nameOfUser)) {
                gitUser.setAvatarLink(avatar);
            }

            trigger.setAuthor(gitUser);
            return Optional.of(trigger);
        }
    }

    private static class GitLabUser {

        @JsonAlias("user_id")
        public String id;

        @JsonAlias("user_name")
        public String name;

        @JsonAlias("user_username")
        public String username;

        @JsonAlias("user_email")
        public String email;

        @JsonAlias("user_avatar")
        public String avatar;
    }

    private static class Commit {

        public String id;

        public String message;

        public String timestamp;

        public String url;

        public GitLabUser author;

        // modified file name list
        public List<String> modified;

        // removed file name list
        public List<String> removed;
    }
}
