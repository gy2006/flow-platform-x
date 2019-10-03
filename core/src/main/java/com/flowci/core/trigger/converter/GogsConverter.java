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
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.trigger.domain.GitEvent;
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

@Log4j2
@Component("gogsConverter")
public class GogsConverter implements TriggerConverter {

    public static final String Header = "x-gogs-event";

    public static final String Ping = "ping";

    public static final String PushOrTag = "push";

    public static final String PR = "pull_request";

    private final Map<String, Function<InputStream, GitTrigger>> mapping =
            ImmutableMap.<String, Function<InputStream, GitTrigger>>builder()
                    .put(PushOrTag, new EventConverter<>(PushOrTagEvent.class))
                    .build();

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Optional<GitTrigger> convert(String event, InputStream body) {
        GitTrigger trigger = mapping.get(event).apply(body);
        return Optional.ofNullable(trigger);
    }

    private class EventConverter<T extends GitEvent> implements Function<InputStream, GitTrigger> {

        private final Class<T> target;

        public EventConverter(Class<T> target) {
            this.target = target;
        }

        @Override
        public GitTrigger apply(InputStream stream) {
            try {
                T event = objectMapper.readValue(stream, target);
                return event.toTrigger();
            } catch (IOException e) {
                log.warn("Unable to parse Gogs ping event");
                return null;
            }
        }
    }

    private static class PushOrTagEvent implements GitEvent {

        public String before;

        public String after;

        public String ref;

        public List<Commit> commits;

        public User pusher;

        @Override
        public GitTrigger toTrigger() {
            if (Objects.isNull(commits) || commits.isEmpty()) {
                throw new ArgumentException("No commits data on Gogs push event");
            }

            GitPushTrigger trigger = new GitPushTrigger();
            trigger.setSource(GitSource.GOGS);
            trigger.setEvent(GitTrigger.GitEvent.PUSH);

            Commit commit = commits.get(0);
            trigger.setCommitId(commit.id);
            trigger.setMessage(commit.message);
            trigger.setCommitUrl(commit.url);
            trigger.setRef(BranchHelper.getBranchName(ref));
            trigger.setTime(commit.timestamp);

            // set commit author info
            trigger.setAuthor(pusher.toGitUser());

            return trigger;
        }
    }

    private static class Commit {

        public String id;

        public String message;

        public String url;

        public User author;

        public User committer;

        public List<String> modified;

        public String timestamp;
    }

    private static class User {

        public int id;

        public String username;

        public String email;

        @JsonAlias("avatar_url")
        public String avatarUrl;

        GitUser toGitUser() {
            return new GitUser()
                    .setEmail(email)
                    .setUsername(username)
                    .setAvatarLink(avatarUrl);
        }
    }
}
