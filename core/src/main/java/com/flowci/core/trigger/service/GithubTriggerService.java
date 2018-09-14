/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.trigger.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.trigger.domain.GitPushTrigger;
import com.flowci.core.trigger.domain.GitTagTrigger;
import com.flowci.core.trigger.domain.GitTrigger.Author;
import com.flowci.core.trigger.domain.GitTrigger.GitSource;
import com.flowci.exception.ArgumentException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service("gitHubTriggerService")
public class GithubTriggerService implements GitTriggerService {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public GitPushTrigger onPush(InputStream stream) {
        try {
            PushObject pushObject = objectMapper.readValue(stream, PushObject.class);
            return pushObject.toTrigger();
        } catch (IOException e) {
            throw new ArgumentException("Unable to parse Github push event data");
        }
    }

    @Override
    public GitTagTrigger onTag(InputStream stream) {
        try {
            TagObject tagObject = objectMapper.readValue(stream, TagObject.class);
            return tagObject.toTrigger();
        } catch (IOException e) {
            throw new ArgumentException("Unable to parse Github tag event data");
        }
    }

    private static class PushObject {

        public String ref;

        public String compare;

        @JsonProperty("head_commit")
        public CommitObject commit;

        public GitPushTrigger toTrigger() {
            if (Objects.isNull(commit)) {
                throw new ArgumentException("On commits data on Github push event");
            }

            GitPushTrigger trigger = new GitPushTrigger();
            trigger.setSource(GitSource.GITHUB);

            trigger.setCommitId(commit.id);
            trigger.setMessage(commit.message);
            trigger.setCommitUrl(commit.url);
            trigger.setCompareUrl(compare);
            trigger.setRef(ref);
            trigger.setTime(commit.timestamp);

            // set commit author info
            Author author = new Author();
            author.setEmail(commit.author.email);
            author.setName(commit.author.name);
            author.setUsername(commit.author.username);
            trigger.setAuthor(author);

            return trigger;
        }
    }

    private static class TagObject extends PushObject {

        public GitTagTrigger toTrigger() {
            GitPushTrigger pushTrigger = super.toTrigger();

            GitTagTrigger tagTrigger = new GitTagTrigger();
            BeanUtils.copyProperties(pushTrigger, tagTrigger, "event");
            return tagTrigger;
        }
    }

    private static class CommitObject {

        public String id;

        public String message;

        public String timestamp;

        public String url;

        public AuthorObject author;
    }

    private static class AuthorObject {

        public String name;

        public String email;

        public String username;

    }
}
