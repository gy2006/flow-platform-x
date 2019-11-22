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
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.trigger.domain.*;
import com.flowci.core.trigger.util.BranchHelper;
import com.flowci.exception.ArgumentException;
import com.flowci.util.StringHelper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Log4j2
@Component("gogsConverter")
public class GogsConverter extends TriggerConverter {

    public static final String Header = "x-gogs-event";

    public static final String Push = "push";

    public static final String Tag = "release";

    public static final String PR = "pull_request";

    private final Map<String, Function<InputStream, GitTrigger>> mapping =
            ImmutableMap.<String, Function<InputStream, GitTrigger>>builder()
                    .put(Push, new EventConverter<>("Push", PushEvent.class))
                    .put(Tag, new EventConverter<>("Tag", ReleaseEvent.class))
                    .put(PR, new EventConverter<>("PR", PrEvent.class))
                    .build();

    @Override
    GitSource getGitSource() {
        return GitSource.GOGS;
    }

    @Override
    Map<String, Function<InputStream, GitTrigger>> getMapping() {
        return mapping;
    }

    // ======================================================
    //      Objects for GitHub
    // ======================================================

    private static class PushEvent implements GitTriggerable {

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
            trigger.setNumOfCommit(commits.size());

            // set commit author info
            trigger.setAuthor(pusher.toGitUser());

            return trigger;
        }
    }

    // Release event
    private static class ReleaseEvent implements GitTriggerable {

        public String action;

        public Release release;

        @Override
        public GitTrigger toTrigger() {
            GitPushTrigger tag = new GitPushTrigger();
            tag.setEvent(GitTrigger.GitEvent.TAG);
            tag.setSource(GitSource.GOGS);

            tag.setRef(release.tagName);
            tag.setMessage(release.name);
            tag.setCommitId(release.id);
            tag.setTime(release.createdAt);
            tag.setCommitUrl(StringHelper.EMPTY);
            tag.setAuthor(release.author.toGitUser());
            tag.setNumOfCommit(0);

            return tag;
        }
    }

    private static class PrEvent implements GitTriggerable {

        final static String ACTION_OPENED = "opened";

        final static String ACTION_CLOSED = "closed";

        public String action;

        @JsonAlias("pull_request")
        public PrBody prBody;

        public User sender;

        @Override
        public GitTrigger toTrigger() {
            GitPrTrigger trigger = new GitPrTrigger();
            setTriggerEvent(trigger);

            trigger.setSource(GitSource.GOGS);
            trigger.setNumber(prBody.number);
            trigger.setBody(prBody.body);
            trigger.setTitle(prBody.title);
            trigger.setUrl(prBody.url);
            trigger.setTime(prBody.mergedAt);
            trigger.setNumOfCommits(StringHelper.EMPTY);
            trigger.setNumOfFileChanges(StringHelper.EMPTY);
            trigger.setMerged(prBody.merged);

            GitPrTrigger.Source head = new GitPrTrigger.Source();
            head.setCommit(StringHelper.EMPTY);
            head.setRef(prBody.headBranch);
            head.setRepoName(prBody.head.fullName);
            head.setRepoUrl(prBody.head.url);
            trigger.setHead(head);

            GitPrTrigger.Source base = new GitPrTrigger.Source();
            base.setCommit(StringHelper.EMPTY);
            base.setRef(prBody.baseBranch);
            base.setRepoName(prBody.base.fullName);
            base.setRepoUrl(prBody.base.url);
            trigger.setBase(base);

            trigger.setSender(sender.toGitUser());

            if (!StringHelper.hasValue(trigger.getTime())) {
                trigger.setTime(StringHelper.EMPTY);
            }

            return trigger;
        }

        private void setTriggerEvent(GitPrTrigger trigger) {
            if (action.equals(ACTION_OPENED)) {
                trigger.setEvent(GitTrigger.GitEvent.PR_OPENED);
                return;
            }

            if (action.equals(ACTION_CLOSED) && prBody.merged) {
                trigger.setEvent(GitTrigger.GitEvent.PR_MERGED);
                return;
            }

            throw new ArgumentException("Cannot handle action {0} from pull request", action);
        }
    }

    private static class PrBody {

        public String id;

        public String number;

        public String title;

        public String body;

        public User user;

        @JsonAlias("html_url")
        public String url;

        @JsonAlias("head_repo")
        public Repo head;

        @JsonAlias("head_branch")
        public String headBranch;

        @JsonAlias("base_repo")
        public Repo base;

        @JsonAlias("base_branch")
        public String baseBranch;

        public boolean merged;

        @JsonAlias("merged_at")
        public String mergedAt;
    }

    private static class Repo {

        public String name;

        @JsonAlias("full_name")
        public String fullName;

        @JsonAlias("html_url")
        public String url;
    }

    private static class Release {

        public String id;

        @JsonAlias("tag_name")
        public String tagName;

        @JsonAlias("target_commitish")
        public String ref;

        // title
        public String name;

        public String body;

        @JsonAlias("created_at")
        public String createdAt;

        public User author;
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

        public String id;

        public String username;

        public String email;

        @JsonAlias("avatar_url")
        public String avatarUrl;

        GitUser toGitUser() {
            return new GitUser()
                    .setId(id)
                    .setEmail(email)
                    .setUsername(username)
                    .setAvatarLink(avatarUrl);
        }
    }
}
