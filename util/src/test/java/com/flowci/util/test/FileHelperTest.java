/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.util.test;

import com.flowci.util.FileHelper;
import org.junit.Assert;
import org.junit.Test;

public class FileHelperTest {

    @Test
    public void should_get_file_name_without_type() {
        String name = FileHelper.getName("hello.log.raw");
        Assert.assertEquals("hello", name);
    }
}
