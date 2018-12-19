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
package com.hello2morrow.sonargraph.integration.jenkins.controller;

import org.kohsuke.stapler.DataBoundConstructor;

public class Metric
{
    private String metricName, metricCategory;

    public Metric()
    {
        this("", "");
    }

    @DataBoundConstructor
    public Metric(final String metricName, final String metricCategory)
    {
        this.metricName = metricName;
        this.metricCategory = metricCategory;
    }

    public String getMetricName()
    {
        return metricName;
    }

    public void setMetricName(final String metricName)
    {
        this.metricName = metricName;
    }

    public String getMetricCategory()
    {
        return metricCategory;
    }

    public void setMetricCategory(final String metricCategory)
    {
        this.metricCategory = metricCategory;
    }

}