/*
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2023 hello2morrow GmbH
 * mailto: support AT hello2morrow DOT com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hello2morrow.sonargraph.integration.jenkins.model;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricId;

public interface IMetricHistoryProvider
{
    /**
     * @return List of data points for a specific metric.
     */
    public List<IDataPoint> readMetricValues(MetricId metric) throws IOException;

    /**
     * Appends all supported metrics for a specific build.
     * @param buildNumber Number of the build where the metric was gathered
     * @param timestamp when the build has been executed
     * @param metricValues map containing the supported metrics and their values for the current build.
     */
    public void writeMetricValues(Integer buildNumber, long timestamp, Map<MetricId, String> metricValues) throws IOException;

    public String getStorageName();
    
}