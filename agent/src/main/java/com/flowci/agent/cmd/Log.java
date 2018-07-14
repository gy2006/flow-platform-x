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

package com.flowci.agent.cmd;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yang
 */
@RequiredArgsConstructor
@AllArgsConstructor
@ToString(of = {"type", "content"})
public final class Log implements Serializable {

    public enum Type {
        STDOUT,
        STDERR,
    }

    @Getter
    private final Type type;

    @Getter
    private final String content;

    @Getter
    @Setter
    private Integer number = 0;

}
