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

package com.flowci.core.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * @author yang
 */
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id"})
public abstract class Mongoable implements Serializable {

    @Id
    @Getter
    @Setter
    @NonNull
    private String id;

    @Getter
    @Setter
    @CreatedDate
    private Date createdAt;

    @Getter
    @Setter
    @LastModifiedDate
    private Date updatedAt;

    @Getter
    @Setter
    private String createdBy;
}
