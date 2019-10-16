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

package com.flowci.core.api.domain;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author yang
 */
@Getter
@Setter
public final class AddStatsItem {

    @NotEmpty
    private String type;

    private Map<String, Float> data = new HashMap<>();

}
