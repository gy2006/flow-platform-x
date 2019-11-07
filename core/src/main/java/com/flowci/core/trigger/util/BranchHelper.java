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

package com.flowci.core.trigger.util;

import com.flowci.util.StringHelper;
import com.google.common.base.Strings;

public abstract class BranchHelper {

    /**
     * Transfer branch name 'refs/heads/master' to `master`
     */
    public static String getBranchName(String ref) {
        if (Strings.isNullOrEmpty(ref)) {
            return StringHelper.EMPTY;
        }

        // find first '/'
        int index = ref.indexOf('/');
        if (index == -1) {
            return StringHelper.EMPTY;
        }

        // find second '/'
        ref = ref.substring(index + 1);
        index = ref.indexOf('/');
        if (index == -1) {
            return StringHelper.EMPTY;
        }

        return ref.substring(index + 1);
    }
}
