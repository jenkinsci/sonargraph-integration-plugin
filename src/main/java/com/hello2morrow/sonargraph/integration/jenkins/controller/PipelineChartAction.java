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
package com.hello2morrow.sonargraph.integration.jenkins.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.hello2morrow.sonargraph.integration.access.foundation.ResultWithOutcome;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.ChartConfigurationFileHandler;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.ChartConfigurationFileHandler.ChartConfiguration;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricId;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricIds;

import hudson.model.Action;
import hudson.model.Job;
import net.sf.json.JSONObject;

public class PipelineChartAction implements Action
{
    private final Job<?, ?> job;

    private final MetricIds metaData;

    private final ChartConfiguration chartConfiguration;
    private final ChartConfigurationFileHandler chartConfigurationFileHandler;

    private static final Map<Job<?, ?>, PipelineChartAction> s_instances = new HashMap<>();

    public static PipelineChartAction getInstance(Job<?, ?> job)
    {
        if (s_instances.get(job) == null)
        {
            s_instances.put(job, new PipelineChartAction(job));
        }
        return s_instances.get(job);
    }

    private PipelineChartAction(Job<?, ?> job)
    {
        this.job = job;
        ResultWithOutcome<MetricIds> metricIds = MetricIdProvider.getMetricIds(job);
        this.metaData = metricIds.getOutcome();
        chartConfigurationFileHandler = new ChartConfigurationFileHandler(job);
        chartConfiguration = chartConfigurationFileHandler.load();
    }

    public boolean isCore()
    {
        return chartConfiguration.core;
    }

    public boolean isJava()
    {
        return chartConfiguration.java;
    }

    public boolean isCplusplus()
    {
        return chartConfiguration.cplusplus;
    }

    public boolean isCsharp()
    {
        return chartConfiguration.csharp;
    }

    public boolean isPython()
    {
        return chartConfiguration.python;
    }

    public void doSubmit(final StaplerRequest req, final StaplerResponse rsp) throws IOException, ServletException
    {
        JSONObject json = req.getSubmittedForm();
        chartConfiguration.core = json.getBoolean("core");
        chartConfiguration.java = json.getBoolean("java");
        chartConfiguration.cplusplus = json.getBoolean("cplusplus");
        chartConfiguration.csharp = json.getBoolean("csharp");
        chartConfiguration.python = json.getBoolean("python");
        
        chartConfigurationFileHandler.store(chartConfiguration);

        rsp.forwardToPreviousPage(req);
    }

    public Collection<String> getChartsForMetrics()
    {
        final List<String> chartsForMetrics = new ArrayList<>();

        for (String next : metaData.getMetricIds().keySet())
        {
            final MetricId nextMetric = metaData.getMetricIds().get(next);
            final String nextProviderId = nextMetric.getProviderId();
            if ((nextProviderId.equals("Core") && chartConfiguration.core) || (nextProviderId.startsWith("Java") && chartConfiguration.java)
                    || (nextProviderId.startsWith("CSharp") && chartConfiguration.csharp)
                    || (nextProviderId.startsWith("CPlusPlus") && chartConfiguration.cplusplus)
                    || (nextProviderId.startsWith("Python") && chartConfiguration.python) || nextProviderId.startsWith("./"))
            {
                chartsForMetrics.add(next);
            }
        }

        return chartsForMetrics;
    }

    /**
     * Method that generates the chart and adds it to the response object to allow jenkins to display it. It is called in
     * SonargraphChartAction/index.jelly in the src attribute of an img tag.
     */
    public void doGetPlot(final StaplerRequest req, final StaplerResponse rsp)
    {
        Plotter.doGetPlot(job, metaData, req, rsp);
    }

    public Job<?, ?> getJob()
    {
        return job;
    }

    /**
     * Icon that will appear next to the link defined by this action.
     */
    @Override
    public String getIconFileName()
    {
        return ConfigParameters.SONARGRAPH_ICON.getValue();
    }

    /**
     * Name of the link for this action
     */
    @Override
    public String getDisplayName()
    {
        return ConfigParameters.ACTION_DISPLAY_CHART.getValue();
    }

    /**
     * Last segment of the url that will lead to this action. e.g https://localhost:8080/jobName/sonargraph
     */
    @Override
    public String getUrlName()
    {
        return ConfigParameters.ACTION_URL_PIPELINE_CHART.getValue();
    }
}