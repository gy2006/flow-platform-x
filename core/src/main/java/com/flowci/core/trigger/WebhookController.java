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

package com.flowci.core.trigger;

import com.flowci.core.flow.domain.Flow;
import com.flowci.core.flow.domain.Yml;
import com.flowci.core.job.domain.Job;
import com.flowci.core.job.domain.Job.Trigger;
import com.flowci.core.job.service.JobService;
import com.flowci.core.trigger.domain.GitPushTrigger;
import com.flowci.core.trigger.domain.GitTrigger;
import com.flowci.core.trigger.domain.GitTrigger.GitEvent;
import com.flowci.core.trigger.service.TriggerService;
import com.flowci.domain.VariableMap;
import com.flowci.exception.NotFoundException;
import com.flowci.tree.Filter;
import com.flowci.tree.Node;
import com.flowci.tree.YmlParser;
import com.google.common.base.Strings;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * @author yang
 */
@Log4j2
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final Map<String, Callable<GitTrigger>> consumerMap = new HashMap<>(1);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private JobService jobService;

    @Autowired
    private TriggerService gitHubTriggerService;

    @PostConstruct
    public void initConsumer() {
        consumerMap.put(GithubEventConsumer.Header, new GithubEventConsumer());
    }

    @PostMapping("/{name}")
    public void gitTrigger(@PathVariable String name) throws Exception {
        GitTrigger trigger = null;

        if (isGitHub()) {
            Callable<GitTrigger> consumer = consumerMap.get(GithubEventConsumer.Header);
            trigger = consumer.call();
            log.info("Github trigger received: {}", trigger);
        }

        if (Objects.isNull(trigger)) {
            return;
        }

        // get related flow and yml
        Flow flow = gitHubTriggerService.get(name);
        Yml yml = gitHubTriggerService.getYml(flow);
        Node root = YmlParser.load(flow.getName(), yml.getRaw());

        if (canStartJob(root, trigger)) {
            VariableMap gitInput = trigger.toVariableMap();
            Job job = jobService.create(flow, yml, getJobTrigger(trigger), gitInput);
            jobService.start(job);
            log.debug("Start job {} from git event {} from {}", job.getId(), trigger.getEvent(), trigger.getSource());
            return;
        }

        log.info("Trigger filter not matched {}", root.getFilter());
    }

    private boolean canStartJob(Node root, GitTrigger trigger) {
        Filter condition = root.getFilter();

        if (trigger.getEvent() == GitEvent.PUSH) {
            GitPushTrigger pushTrigger = (GitPushTrigger) trigger;
            return condition.isMatchBranch(pushTrigger.getRef());
        }

        if (trigger.getEvent() == GitEvent.TAG) {
            GitPushTrigger tagTrigger = (GitPushTrigger) trigger;
            return condition.isMatchTag(tagTrigger.getRef());
        }

        return true;
    }

    /**
     * Convert git trigger to job trigger
     */
    private Trigger getJobTrigger(GitTrigger trigger) {
        if (trigger.getEvent() == GitEvent.PUSH) {
            return Trigger.PUSH;
        }

        if (trigger.getEvent() == GitEvent.TAG) {
            return Trigger.TAG;
        }

        if (trigger.getEvent() == GitEvent.PR_OPEN) {
            return Trigger.PR_OPEN;
        }

        if (trigger.getEvent() == GitEvent.PR_CLOSE) {
            return Trigger.PR_CLOSE;
        }

        throw new NotFoundException("Cannot found related job trigger for {0}", trigger.getEvent().name());
    }

    private boolean isGitHub() {
        String event = request.getHeader(GithubEventConsumer.Header);
        return !Strings.isNullOrEmpty(event);
    }

    private class GithubEventConsumer implements Callable<GitTrigger> {

        private static final String Header = "X-GitHub-Event";

        private static final String Ping = "ping";

        private static final String PushOrTag = "push";

        private static final String PR = "pull_request";

        @Override
        public GitTrigger call() throws Exception {
            String event = request.getHeader(Header);
            ServletInputStream in = request.getInputStream();

            if (event.equals(PushOrTag)) {
                return gitHubTriggerService.onPushOrTag(in);
            }

            if (event.equals(PR)) {
                return gitHubTriggerService.onPullRequest(in);
            }

            if (event.equals(Ping)) {
                log.info("Ping event from github.com");
                return null;
            }

            return null;
        }
    }
}
