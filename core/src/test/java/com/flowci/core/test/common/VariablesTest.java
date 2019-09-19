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

package com.flowci.core.test.common;

import com.flowci.core.common.domain.Variables;
import com.flowci.domain.Variable;
import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class VariablesTest {

    @Test
    public void should_load_var_rules_from_resource() {
        ImmutableList<Variable> rules = Variables.RULES;

        Assert.assertNotNull(rules);
        Assert.assertTrue(rules.size() > 0);
    }

}
