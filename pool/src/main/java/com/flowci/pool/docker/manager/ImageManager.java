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

/**
 * @author yang
 */
public interface ImageManager {

    /**
     * Pull image
     */
    boolean pull(DockerConfig config, String image);

    /**
     * Delete image
     */
    boolean remove(DockerConfig config, String imageId);

}
