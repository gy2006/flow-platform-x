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

package com.flowci.pool.docker.manager;

import com.flowci.pool.docker.DockerConfig;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.PullResponseItem;
import java.io.Closeable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * @author yang
 */
@Component
public class ImageManagerImpl implements ImageManager {

    @Override
    public boolean pull(DockerConfig config, String image) {
        DockerClient client = config.getClient();

        final AtomicBoolean result = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        client.pullImageCmd(image).exec(new PullResultCallback(latch, result));

        try {
            latch.await();
            return result.get();
        } catch (InterruptedException e) {
            return false;
        }
    }

    @Override
    public boolean remove(DockerConfig config, String imageId) {
        try {
            config.getClient().removeImageCmd(imageId).withForce(true).exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    private class PullResultCallback implements ResultCallback<PullResponseItem> {

        private final CountDownLatch latch;

        private final AtomicBoolean result;

        PullResultCallback(CountDownLatch latch, AtomicBoolean result) {
            this.latch = latch;
            this.result = result;
        }

        @Override
        public void onStart(Closeable closeable) {

        }

        @Override
        public void onNext(PullResponseItem object) {

        }

        @Override
        public void onError(Throwable throwable) {
            result.set(Boolean.FALSE);
            latch.countDown();
        }

        @Override
        public void onComplete() {
            result.set(Boolean.TRUE);
            latch.countDown();
        }

        @Override
        public void close() {

        }
    }
}
