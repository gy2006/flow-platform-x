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

package com.flowci.core.api.service;

import com.flowci.core.api.domain.CreateJobSummary;
import com.flowci.core.credential.domain.Credential;
import com.flowci.core.job.domain.JobSummary;
import com.flowci.core.flow.domain.StatsCounter;
import com.flowci.core.flow.domain.StatsItem;

public interface OpenRestService {

    Credential getCredential(String name);

    StatsItem saveStatsForFlow(String flowName, String statsType, StatsCounter counter);

    JobSummary saveJobSummary(String flowName, long buildNumber, CreateJobSummary body);
}
