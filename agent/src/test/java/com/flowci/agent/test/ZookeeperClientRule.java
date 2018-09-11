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

package com.flowci.agent.test;

import com.flowci.zookeeper.ZookeeperClient;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.Getter;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author yang
 */
public class ZookeeperClientRule implements TestRule {

    @Getter
    private final ZookeeperClient client;

    private final Executor executor = Executors.newSingleThreadExecutor();

    public ZookeeperClientRule(String zkHost) {
        client = new ZookeeperClient(zkHost, 10, 30, executor);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    start();
                    base.evaluate();
                } finally {
                    client.close();
                }
            }
        };
    }

    private void start() {
        try {
            client.start();
        } catch (Throwable ignore) {

        }
    }
}
