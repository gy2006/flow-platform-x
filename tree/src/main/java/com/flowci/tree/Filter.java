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

package com.flowci.tree;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Git Trigger Condition
 *
 * @author yang
 */
@Getter
@Setter
public class Filter implements Serializable {

    /**
     * Condition on branches
     */
    private List<String> branches = new LinkedList<>();

    /**
     * Condition on tags
     */
    private List<String> tags = new LinkedList<>();

    /**
     * Env export filter
     */
    private List<String> exports = new LinkedList<>();

    public boolean available() {
        return !branches.isEmpty() || !tags.isEmpty() || !exports.isEmpty();
    }

    public boolean isMatchBranch(String branch) {
        if (branches.isEmpty()) {
            return true;
        }

        for (String re : branches) {
            if (branch.matches(re)) {
                return true;
            }
        }

        return false;
    }

    public boolean isMatchTag(String tag) {
        if (branches.isEmpty()) {
            return true;
        }

        for (String re : tags) {
            if (tag.matches(re)) {
                return true;
            }
        }

        return false;
    }
}
