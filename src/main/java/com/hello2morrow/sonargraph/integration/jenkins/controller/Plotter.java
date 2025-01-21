/*
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2025 hello2morrow GmbH
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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import org.jfree.chart.JFreeChart;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.model.AbstractPlot;
import com.hello2morrow.sonargraph.integration.jenkins.model.IMetricHistoryProvider;
import com.hello2morrow.sonargraph.integration.jenkins.model.TimeSeriesPlot;
import com.hello2morrow.sonargraph.integration.jenkins.model.XYLineAndShapePlot;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.CSVFileHandler;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricId;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricIds;

import hudson.model.Job;
import hudson.util.Graph;

public class Plotter
{
    private static final String TYPE_PARAMETER = "type";
    private static final String METRIC_PARAMETER = "metric";

    private static final String SHORT_TERM = "shortterm";
    private static final String LONG_TERM = "longterm";

    private static final Integer defaultGraphicWidth = 400;
    private static final Integer defaultGraphicHeight = 250;

    private static final int MAX_DATA_POINTS_SHORT_TERM = 25;
    private static final int MAX_DATA_POINTS_LONG_TERM = 300;

    private static final String BUILD = "Build";
    private static final String DATE = "Date";
    
    private Plotter()
    {
        // no instances
    }
    
    /**
     * Method that generates the chart and adds it to the response object to allow jenkins to display it. It is called in
     * SonargraphChartAction/index.jelly in the src attribute of an img tag.
     * @param metaData 
     */
    public static void doGetPlot(final Job<?, ?> job, final MetricIds metaData, final StaplerRequest req, final StaplerResponse rsp)
    {
        final Map<String, String[]> parameterMap = req.getParameterMap();
        final String metricName = getSimpleValue(METRIC_PARAMETER, parameterMap);

        if (metricName == null)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "No metric specified for creating a plot.");
            return;
        }

        final MetricId metric = metaData.getMetricId(metricName);
        if (metric == null)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Specified metric '" + metricName + "' is not supported.");
            return;
        }

        final File csvFile = new File(job.getRootDir(), ConfigParameters.METRIC_HISTORY_CSV_FILE_PATH.getValue());
        SonargraphLogger.INSTANCE.log(Level.FINE,
                "Generating chart for metric '" + metricName + "'. Reading values from '" + csvFile.getAbsolutePath() + "'");
        final IMetricHistoryProvider csvFileHandler = new CSVFileHandler(csvFile, metaData);

        final String type = getSimpleValue(TYPE_PARAMETER, parameterMap);
        AbstractPlot plot;
        String xAxisLabel;
        int maxDataPoints;
        if ((type == null) || type.equals(SHORT_TERM))
        {
            plot = new XYLineAndShapePlot(csvFileHandler);
            xAxisLabel = BUILD;
            maxDataPoints = MAX_DATA_POINTS_SHORT_TERM;
        }
        else if (type.equals(LONG_TERM))
        {
            plot = new TimeSeriesPlot(csvFileHandler, MAX_DATA_POINTS_SHORT_TERM);
            xAxisLabel = DATE;
            maxDataPoints = MAX_DATA_POINTS_LONG_TERM;
        }
        else
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Chart type '" + type + "' is not supported!");
            return;
        }

        final JFreeChart chart = plot.createXYChart(metric, xAxisLabel, maxDataPoints, true);
        final Graph graph = new Graph(plot.getTimestampOfLastDisplayedPoint(), defaultGraphicWidth, defaultGraphicHeight)
        {
            @Override
            protected JFreeChart createGraph()
            {
                return chart;
            }
        };
        try
        {
            graph.doPng(req, rsp);
        }
        catch (final IOException ioe)
        {
            SonargraphLogger.INSTANCE.log(Level.WARNING, "Error generating the graphic for metric '" + metric.getName() + "'");
        }
    }
    
    private static String getSimpleValue(final String parameterName, final Map<String, String[]> params)
    {
        assert parameterName != null && parameterName.length() > 0 : "Parameter 'parameterName' of method 'getValue' must not be empty";
        assert params != null : "Parameter 'params' of method 'getValue' must not be null";

        final String[] value = params.get(parameterName);
        if (value == null || value.length == 0)
        {
            return null;
        }

        return value[0];
    }
}
