/*
 * Copyright 2018 fir.im
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

package com.flowci.zookeeper;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;

/**
 * @author yang
 */
public class ZookeeperClient implements AutoCloseable {

    private final static int RetryBetweenInMs = 1000 * 10;

    private final CuratorFramework client;

    private final int timeout;

    public ZookeeperClient(String connection, int retryTimes, int timeOutInSeconds) {
        this.timeout = timeOutInSeconds;
        RetryPolicy policy = new RetryNTimes(retryTimes, RetryBetweenInMs);
        client = CuratorFrameworkFactory.newClient(connection, policy);
    }

    public boolean start() {
        try {
            client.start();
            return client.blockUntilConnected(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
            return false;
        }
    }

    public boolean exist(String path) {
        try {
            return client.checkExists().forPath(path) != null;
        } catch (Throwable e) {
            throw new ZookeeperException("Cannot check existing for path: {0}", e.getMessage());
        }
    }

    public String create(CreateMode mode, String path, byte[] data) {
        if (data == null) {
            data = new byte[0];
        }

        try {
            return client.create()
                .withMode(mode)
                .forPath(path, data);
        } catch (Throwable e) {
            throw new ZookeeperException("Fail to create node: {0}", e.getMessage());
        }
    }

    public void delete(String path, boolean isDeleteChildren) {
        try {
            if (!exist(path)) {
                return;
            }

            DeleteBuilder builder = client.delete();

            if (isDeleteChildren) {
                builder.guaranteed().deletingChildrenIfNeeded().forPath(path);
                return;
            }

            builder.guaranteed().forPath(path);
        } catch (Throwable e) {
            throw new ZookeeperException("Fail to delete node of path: {0}", e.getMessage());
        }
    }

    public byte[] get(String path) {
        if (!exist(path)) {
            throw new ZookeeperException("Node path {0} does not existed", path);
        }

        try {
            return client.getData().forPath(path);
        } catch (Throwable e) {
            throw new ZookeeperException("Fail to get data for node: {0}", e.getMessage());
        }
    }

    public void set(String path, byte[] data) {
        if (!exist(path)) {
            throw new ZookeeperException("Node path {} does not existed", path);
        }

        try {
            client.setData().forPath(path, data);
        } catch (Throwable e) {
            throw new ZookeeperException("Fail to set data for node: {0}", e.getMessage());
        }
    }

    public void lock(String path, Consumer<String> consumer) {
        InterProcessMutex lock = new InterProcessMutex(client, path);

        try {
            if (!lock.acquire(0, TimeUnit.SECONDS)) {
                throw new ZookeeperException("Cannot acquire the lock on path: " + path);
            }

            consumer.accept(path);
        } catch (Exception e) {
            throw new ZookeeperException("Cannot acquire the lock on path: " + path);
        } finally {
            if (lock.isAcquiredInThisProcess()) {
                try {
                    lock.release();
                } catch (Exception ignored) {

                }
            }
        }
    }

    @Override
    public void close() {
        if (Objects.isNull(client)) {
            return;
        }

        client.close();
    }
}
