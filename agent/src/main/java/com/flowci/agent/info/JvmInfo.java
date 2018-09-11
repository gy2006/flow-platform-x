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

package com.flowci.agent.info;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import lombok.Data;
import org.springframework.boot.actuate.info.Info.Builder;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author yang
 */
@Component
public class JvmInfo implements InfoContributor {

    @Override
    public void contribute(Builder builder) {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();

        Runtime runtime = Runtime.getRuntime();

        Jvm jvmInfo = Jvm.of(
            System.getProperty("java.version"),
            runtime.availableProcessors(),
            runtime.totalMemory(),
            runtime.freeMemory(),
            StringUtils.collectionToCommaDelimitedString(arguments)
        );

        builder.withDetail("jvm", jvmInfo);
    }

    @Data(staticConstructor = "of")
    private static class Jvm {

        private final String version;

        private final int cpu;

        private final long totalMemory;

        private final long freeMemory;

        private final String arguments;

    }
}
