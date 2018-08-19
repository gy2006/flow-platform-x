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

package com.flowci.util.test;

import com.flowci.util.UnixHelper;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class UnixHelperTest {

    @Test
    public void should_parse_unix_env() {
        String home = UnixHelper.parseEnv("${HOME}");
        Assert.assertEquals(System.getenv("HOME"), home);

        home = UnixHelper.parseEnv("$HOME");
        Assert.assertEquals(System.getenv("HOME"), home);

        home = UnixHelper.parseEnv("${HOME");
        Assert.assertNull(home);
    }

    @Test
    public void should_replace_path_with_env() {
        // single placeholder
        Path path = UnixHelper.replacePathWithEnv("${HOME}/test");
        Path expected = Paths.get(System.getenv("HOME"), "test");
        Assert.assertEquals(expected, path);

        // two placeholder
        path = UnixHelper.replacePathWithEnv("${HOME}/${HOME}");
        expected = Paths.get(System.getenv("HOME"), System.getenv("HOME"));
        Assert.assertEquals(expected, path);

        // no placeholder
        path = UnixHelper.replacePathWithEnv("/etc/flow.ci");
        expected = Paths.get("/etc/flow.ci");
        Assert.assertEquals(expected, path);
    }

}
