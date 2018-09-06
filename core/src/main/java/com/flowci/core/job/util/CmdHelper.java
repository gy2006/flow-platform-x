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

package com.flowci.core.job.util;

import com.flowci.core.job.domain.Job;
import com.flowci.domain.Cmd;
import com.flowci.domain.CmdType;
import com.flowci.domain.VariableMap;
import com.flowci.tree.Node;
import com.google.common.collect.Lists;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.UUID;
import lombok.Data;

/**
 * @author yang
 */
public class CmdHelper {

    @Data
    public static class CmdID {

        private final String jobId;

        private final String nodePath;

        @Override
        public String toString() {
            String cmdId = MessageFormat.format("{0}-{1}", jobId, nodePath);
            return Base64.getEncoder().encodeToString(cmdId.getBytes());
        }
    }

    public static CmdID parseID(String id) {
        byte[] decode = Base64.getDecoder().decode(id);
        String idString = new String(decode);
        int index = idString.indexOf('-');

        return new CmdID(idString.substring(0, index), idString.substring(index + 1));
    }

    public static CmdID createId(Job job, Node node) {
        return new CmdID(job.getId(), node.getPath().getPathInStr());
    }

    public static Cmd createShell(Job job, Node node) {
        VariableMap variables = node.getEnvironments().merge(job.getContext());

        Cmd cmd = new Cmd(createId(job, node).toString(), CmdType.SHELL);
        cmd.setInputs(variables);
        cmd.setScripts(Lists.newArrayList(node.getScript()));
        return cmd;
    }

    public static Cmd createKill() {
        return new Cmd(UUID.randomUUID().toString(), CmdType.KILL);
    }

    private CmdHelper() {
    }
}
