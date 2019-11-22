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
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.trigger.domain.*;
import com.flowci.core.trigger.domain.GitTrigger.GitEvent;
import com.flowci.core.trigger.util.BranchHelper;
import com.flowci.exception.ArgumentException;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author yang
 */
@Log4j2
@Component("gitLabConverter")
public class GitLabConverter extends TriggerConverter {

    public static final String Header = "x-gitlab-event";

    public static final String Push = "Push Hook";

    public static final String Tag = "Tag Push Hook";

    public static final String PR = "Merge Request Hook";

    private final Map<String, Function<InputStream, GitTrigger>> mapping =
        ImmutableMap.<String, Function<InputStream, GitTrigger>>builder()
            .put(Push, new EventConverter<>("Push", PushOrTagEvent.class))
            .put(Tag, new EventConverter<>("Tag", PushOrTagEvent.class))
            .put(PR, new EventConverter<>("PR", PrEvent.class))
            .build();

    @Override
    GitSource getGitSource() {
        return GitSource.GITLAB;
    }

    @Override
    Map<String, Function<InputStream, GitTrigger>> getMapping() {
        return mapping;
    }

    // ======================================================
    //      Objects for GitLab
    // ======================================================

    private abstract static class Event implements GitTriggerable {

        @JsonProperty("event_name")
        public String name;
    }

    private static class PushOrTagEvent extends Event {

        final static String PushEvent = "push";

        final static String TagEvent = "tag_push";

        public String before;

        public String after;

        public String ref;

        public String message;

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
        public GitTrigger toTrigger() {
            if (commits == null || commits.size() == 0) {
                throw new ArgumentException("No commits data on GitLab push event");
            }

            GitPushTrigger trigger = new GitPushTrigger();
            trigger.setSource(GitSource.GITLAB);
            trigger.setEvent(getEvent());

            trigger.setCommitId(after);
            trigger.setMessage(message);

            ObjectsHelper.ifNotNull(commits, val -> {
                if (val.size() == 0) {
                    return;
                }

                Commit topCommit = val.get(0);

                // get message from commit if no message available
                if (Strings.isNullOrEmpty(message)) {
                    trigger.setMessage(topCommit.message);
                }

                trigger.setCommitUrl(topCommit.url);
                trigger.setRef(BranchHelper.getBranchName(ref));
                trigger.setTime(topCommit.timestamp);
                trigger.setNumOfCommit(val.size());

                // set commit author info
                GitUser gitUser = new GitUser()
                    .setEmail(topCommit.author.email);

                if (Objects.equals(topCommit.author.name, nameOfUser)) {
                    gitUser.setUsername(username);
                    gitUser.setAvatarLink(avatar);
                }

                trigger.setAuthor(gitUser);
            });


            return trigger;
        }

        private GitEvent getEvent() {
            if (name.equals(TagEvent)) {
                return GitEvent.TAG;
            }

            if (name.equals(PushEvent)) {
                return GitEvent.PUSH;
            }

            throw new ArgumentException("Unsupported event '{0}' from gitlab", name);
        }
    }

    private static class PrEvent extends Event {

        final static String PrOpened = "opened";

        final static String PrMerged = "merged";

        public GitLabUser user;

        @JsonAlias("object_attributes")
        public PrAttributes attributes;

        @Override
        public GitTrigger toTrigger() {
            GitPrTrigger trigger = new GitPrTrigger();
            setTriggerEvent(trigger);

            trigger.setSource(GitSource.GITLAB);
            trigger.setNumber(attributes.number);
            trigger.setBody(attributes.description);
            trigger.setTitle(attributes.title);
            trigger.setUrl(attributes.url);
            trigger.setTime(attributes.createdAt);
            trigger.setNumOfCommits("0");
            trigger.setNumOfFileChanges("0");
            trigger.setMerged(attributes.state.equals(PrMerged));

            GitPrTrigger.Source head = new GitPrTrigger.Source();
            head.setCommit(attributes.lastCommit.id);
            head.setRef(attributes.sourceBranch);
            head.setRepoName(attributes.source.name);
            head.setRepoUrl(attributes.source.webUrl);
            trigger.setHead(head);

            GitPrTrigger.Source base = new GitPrTrigger.Source();
            base.setCommit(StringHelper.EMPTY);
            base.setRef(attributes.targetBranch);
            base.setRepoName(attributes.target.name);
            base.setRepoUrl(attributes.target.webUrl);
            trigger.setBase(base);

            GitUser sender = new GitUser()
                .setUsername(user.username)
                .setAvatarLink(user.avatar);
            trigger.setSender(sender);

            return trigger;
        }

        private void setTriggerEvent(GitPrTrigger trigger) {
            if (attributes.state.equals(PrOpened)) {
                trigger.setEvent(GitTrigger.GitEvent.PR_OPENED);
                return;
            }

            if (attributes.state.equals(PrMerged)) {
                trigger.setEvent(GitTrigger.GitEvent.PR_MERGED);
                return;
            }

            throw new ArgumentException("Unsupported pr action '{0}' from gitlab", attributes.state);
        }
    }

    private static class PrAttributes {

        public String title;

        @JsonAlias("created_at")
        public String createdAt;

        public String description;

        public String state;

        public String url;

        @JsonAlias("iid")
        public String number;

        @JsonAlias("source_branch")
        public String sourceBranch;

        public Project source;

        @JsonAlias("target_branch")
        public String targetBranch;

        public Project target;

        @JsonAlias("last_commit")
        public Commit lastCommit;

    }

    private static class Project {

        public String id;

        public String name;

        @JsonAlias("web_url")
        public String webUrl;
    }

    private static class GitLabUser {

        @JsonAlias("user_id")
        public String id;

        @JsonAlias({"user_name", "name"})
        public String name;

        @JsonAlias({"user_username", "username"})
        public String username;

        @JsonAlias("user_email")
        public String email;

        @JsonAlias({"user_avatar", "avatar_url"})
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
