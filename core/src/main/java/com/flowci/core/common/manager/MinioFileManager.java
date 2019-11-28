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

package com.flowci.core.common.manager;

import com.flowci.core.common.domain.Pathable;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Minio storage manager,
 * instance created on @see com.flowci.core.common.config.StorageConfig
 */
public class MinioFileManager implements FileManager {

    private static final ByteArrayInputStream EmptyStream = new ByteArrayInputStream(new byte[0]);

    private static final String Separator = "/";

    @Autowired
    private MinioClient minioClient;

    /**
     * First Pathable is bucket, rest of them is object with name "x/y/z/"
     *
     * @param objs
     * @return
     * @throws IOException
     */
    @Override
    public String create(Pathable... objs) throws IOException {
        try {
            String bucketName = objs[0].pathName();

            if (!minioClient.bucketExists(bucketName)) {
                minioClient.makeBucket(bucketName);
            }

            String objectName = getObjectName(objs);
            minioClient.putObject(bucketName, objectName, EmptyStream, 0L, null, null, null);

            return bucketName + Separator + objectName;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    @Override
    public String delete(Pathable... objs) throws IOException {
        return null;
    }

    @Override
    public boolean exist(Pathable... objs) {
        return false;
    }

    private static String getObjectName(Pathable... objs) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < objs.length; i++) {
            Pathable item = objs[i];
            builder.append(item.pathName()).append(Separator);
        }
        return builder.toString();
    }
}
