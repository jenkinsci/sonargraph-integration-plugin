/*
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2018 hello2morrow GmbH
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
package com.hello2morrow.sonargraph.integration.jenkins.persistence;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MetricIdsTest
{
    @Test
    public void testMetricIdsToJson()
    {
        final MetricIds metricIds = new MetricIds();
        final MetricId first = new MetricId("first", "First Metric Id", false);
        metricIds.addMetricId(first);
        final MetricId second = new MetricId("second", "Second Metric Id", false, "Cycle");
        metricIds.addMetricId(second);
        final MetricId third = new MetricId("third", "Third Metric Id", false, "Cycle", "Size", "Robert C. Martin");
        metricIds.addMetricId(third);
        final String jsonString = MetricIds.toJSON(metricIds);
        final MetricIds fromString = MetricIds.fromJSON(jsonString);
        assertEquals(metricIds, fromString);
    }
}
