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

package com.flowci.util.test;

import com.flowci.util.PatternHelper;
import java.util.regex.Matcher;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class PatternHelperTest {

    @Test
    public void should_match_web_url() {
        Matcher matcher = PatternHelper.WEB_URL.matcher("http://flow.ci");
        Assert.assertTrue(matcher.find());

        matcher = PatternHelper.WEB_URL.matcher("https://192.168.0.1:8080/");
        Assert.assertTrue(matcher.find());

        matcher = PatternHelper.WEB_URL.matcher("ttp://flow.ci");
        Assert.assertFalse(matcher.find());
    }

    @Test
    public void should_match_git_url() {
        Matcher matcher = PatternHelper.GIT_URL.matcher("user@host.com:path/to/repo.git");
        Assert.assertTrue(matcher.find());

        matcher = PatternHelper.GIT_URL.matcher("ssh://user@server/project.git");
        Assert.assertTrue(matcher.find());

        matcher = PatternHelper.GIT_URL.matcher("git@server:project.git");
        Assert.assertTrue(matcher.find());

        matcher = PatternHelper.GIT_URL.matcher("git@server:project");
        Assert.assertFalse(matcher.find());
    }

    @Test
    public void should_match_email() {
        Matcher matcher = PatternHelper.EMAIL_ADDRESS.matcher("user@host.com");
        Assert.assertTrue(matcher.find());

        matcher = PatternHelper.EMAIL_ADDRESS.matcher("user@hostcom");
        Assert.assertFalse(matcher.find());
    }

}
