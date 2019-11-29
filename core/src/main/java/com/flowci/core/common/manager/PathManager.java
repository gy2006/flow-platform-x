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

import com.flowci.store.Pathable;
import com.flowci.util.FileHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;

/**
 * Create, Delete path for pathable obj
 *
 * @author yang
 */
@Component
public class PathManager {

    @Autowired
    private Path flowDir;

    public Path create(Pathable... objs) throws IOException {
        Path dir = connect(flowDir, objs);
        return FileHelper.createDirectory(dir);
    }

    public Path delete(Pathable... objs) throws IOException {
        Path dir = connect(flowDir, objs);
        FileSystemUtils.deleteRecursively(dir);
        return dir;
    }

    // Job log path
    public Path log(Pathable... objs) throws IOException {
        Path dir = connect(flowDir, objs);
        return FileHelper.createDirectory(Paths.get(dir.toString(), "logs"));
    }

    public boolean exist(Pathable... objs) {
        Path dir = connect(flowDir, objs);
        return Files.exists(dir);
    }

    private static Path connect(Path base, Pathable... objs) {
        Path path = base;

        for (Pathable item : objs) {
            path = Paths.get(path.toString(), item.pathName());
        }

        return path;
    }
}
