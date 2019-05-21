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

package com.flowci.core.helper;

import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.atomic.AtomicBoolean;

public class PeriodicRunner {

    private final TaskExecutor executor;

    private final int periodInSecond;

    private AtomicBoolean isStop;

    public PeriodicRunner(int periodInSecond, String name) {
        this.periodInSecond = periodInSecond;
        this.executor = ThreadHelper.createTaskExecutor(1, 1, 1, name);
        this.isStop = new AtomicBoolean(false);
    }

    public void start(Runnable task) {
        long timeout = periodInSecond * 1000;

        executor.execute(() -> {
            for (; !isStop.get(); ) {
                try {
                    task.run();
                } finally {
                    ThreadHelper.sleep(timeout);
                }
            }
        });
    }

    public void stop() {
        isStop.set(true);
    }
}
