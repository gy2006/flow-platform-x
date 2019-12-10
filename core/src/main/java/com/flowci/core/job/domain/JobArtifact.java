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

package com.flowci.core.job.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.store.Pathable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.InputStream;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Document(collection = "job_artifact")
public class JobArtifact extends JobOutput {

    public static final Pathable ArtifactPath = () -> "artifacts";

    @JsonIgnore
    @Transient
    private InputStream src;
}
