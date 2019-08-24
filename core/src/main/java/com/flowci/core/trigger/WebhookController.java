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

import com.flowci.core.trigger.converter.GitHubConverter;
import com.flowci.core.trigger.converter.TriggerConverter;
import com.flowci.core.trigger.domain.GitTrigger;
import com.flowci.core.trigger.service.TriggerService;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yang
 */
@Log4j2
@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private TriggerService triggerService;

    @Autowired
    private TriggerConverter githubConverter;

    @PostMapping("/{name}")
    public void gitTrigger(@PathVariable String name) throws IOException {
        if (isGitHub()) {
            String event = request.getHeader(GitHubConverter.Header);

            Optional<GitTrigger> optional = githubConverter.convert(event, request.getInputStream());

            if (!optional.isPresent()) {
                return;
            }

            log.info("Github trigger received: {}", optional.get());
            triggerService.startJob(name, optional.get());
        }
    }

    private boolean isGitHub() {
        String event = request.getHeader(GitHubConverter.Header);
        return !Strings.isNullOrEmpty(event);
    }
}
