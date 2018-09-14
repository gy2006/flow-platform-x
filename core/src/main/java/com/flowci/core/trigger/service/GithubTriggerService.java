/*
 * Copyright 2018 fir.im
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowci.core.trigger.domain.GitPushTrigger;
import com.flowci.core.trigger.domain.GitTrigger.Author;
import com.flowci.core.trigger.domain.GitTrigger.GitSource;
import com.flowci.exception.ArgumentException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yang
 */
@Service("gitHubTriggerService")
public class GithubTriggerService implements GitTriggerService {

    @Autowired
    private ObjectMapper objectMapper;

    public GitPushTrigger onPush(InputStream stream) {
        try {
            PushObject pushObject = objectMapper.readValue(stream, PushObject.class);
            return pushObject.toTrigger();
        } catch (IOException e) {
            throw new ArgumentException("Unable to parse Github push event data");
        }
    }

    private static class PushObject {

        public String ref;

        public String compare;

        public List<CommitObject> commits;

        public GitPushTrigger toTrigger() {
            if (commits.isEmpty()) {
                throw new ArgumentException("On commits data on Github push event");
            }

            CommitObject commit = commits.get(0);

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
