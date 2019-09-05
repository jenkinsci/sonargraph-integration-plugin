/*
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2019 hello2morrow GmbH
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

import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricId;

public abstract class AbstractPlot
{
    private static final Color BACK_GROUND_COLOR = Color.WHITE;
    private static final Color GRID_LINE_COLOR = Color.BLACK;
    protected static final Paint DATA_COLOR = new Color(0, 179, 0);
    private double m_minimumValue = Double.MAX_VALUE;
    private double m_maximumValue = 0.0;
    private int m_datasetSize = 0;
    private long m_timestampOfLastDisplayedPoint = -1;

    protected IMetricHistoryProvider m_datasetProvider;

    public AbstractPlot(final IMetricHistoryProvider datasetProvider)
    {
        super();
        assert datasetProvider != null : "Parameter 'datasetProvider' of method 'DiscreteLinePlot' must not be null";
        m_datasetProvider = datasetProvider;
    }

    /**
     * Creates a chart for a Sonargraph metric
     * @param categoryName Name for the X-Axis, representing a category
     * @return Chart built with the given parameters.
     */
    public final JFreeChart createXYChart(final MetricId metric, final String categoryName, final int maximumNumberOfDataPoints,
            final boolean hideLegend)
    {
        XYDataset dataset = null;
        try
        {
            dataset = createXYDataset(metric, maximumNumberOfDataPoints);
        }
        catch (final IOException ioe)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Failed to read metrics from data file '" + m_datasetProvider.getStorageName() + "'", ioe);
        }
        final JFreeChart chart = createChartInternal(metric.getName(), categoryName, metric.getName(), dataset);
        final XYPlot plot = (XYPlot) chart.getPlot();

        int dataPoints = 0;
        if (dataset == null)
        {
            plot.setNoDataMessage("There was an error loading data for metric '" + metric.getName() + "'");
        }
        else
        {
            for (int i = 0; i < dataset.getSeriesCount(); i++)
            {
                dataPoints += dataset.getItemCount(i);
            }
            if (dataPoints == 0)
            {
                plot.setNoDataMessage("No data found for metric '" + metric.getName() + "'");
            }
        }
        if ((dataset == null) || (dataPoints == 0))
        {
            plot.setNoDataMessagePaint(Color.RED);
            plot.setDomainGridlinesVisible(false);
            plot.setRangeGridlinesVisible(false);
        }
        applyRendering(plot);
        setRangeAxis(!metric.isFloat(), plot);
        applyStandardPlotColors(plot);
        if (hideLegend)
        {
            chart.removeLegend();
        }
        return chart;
    }

    /**
     * Creates a XYDataset from a CSV file.
     */
    protected XYDataset createXYDataset(final MetricId metric, final int maximumNumberOfDataPoints) throws IOException
    {
        final XYSeries xySeries = new XYSeries(metric.getName());
        List<IDataPoint> dataset = m_datasetProvider.readMetricValues(metric);

        final int size = dataset.size();
        SonargraphLogger.INSTANCE
                .fine(size + " data points found for metric '" + metric.getName() + "' in file '" + m_datasetProvider.getStorageName() + "'");

        int maxSize = 0;
        if (maximumNumberOfDataPoints > 0)
        {
            maxSize = maximumNumberOfDataPoints;
        }
        if ((maxSize != 0) && (size > maxSize))
        {
            dataset = dataset.subList(size - maxSize, size);
        }

        BuildDataPoint point = null;
        for (final IDataPoint datapoint : dataset)
        {
            if (datapoint instanceof InvalidDataPoint)
            {
                // We could create a gap in the graph by adding null:
                // xySeries.add(datapoint.getX(), null);
                continue;
            }
            else if (datapoint instanceof BuildDataPoint)
            {
                point = (BuildDataPoint) datapoint;
                xySeries.add(point.getX(), point.getY());
                checkMinMaxYValue(point.getY());
            }
        }
        if (point != null)
        {
            setTimestampOfLastDisplayedPoint(point.getTimestamp());
        }

        //SG-325: We cannot use JFreeChart methods of version 1.0.14
        //        setMinimumValue(xySeries.getMinY());
        //        setMaximumValue(xySeries.getMaxY());
        setDataSetSize(xySeries.getItemCount());
        return new XYSeriesCollection(xySeries);
    }

    protected abstract JFreeChart createChartInternal(String chartTitle, String categoryName, String yAxisName, XYDataset dataset);

    protected abstract void applyRendering(XYPlot plot);

    protected void setDataSetSize(final int itemCount)
    {
        m_datasetSize = itemCount;
    }

    protected void checkMinMaxYValue(final double value)
    {
        if (m_minimumValue > value)
        {
            m_minimumValue = value;
        }
        if (m_maximumValue < value)
        {
            m_maximumValue = value;
        }
    }

    protected double getMinimumValue()
    {
        return m_minimumValue;
    }

    protected double getMaximumValue()
    {
        return m_maximumValue;
    }

    protected void setMinimumValue(final double minimumValue)
    {
        m_minimumValue = minimumValue;
    }

    protected void setMaximumValue(final double maximumValue)
    {
        m_maximumValue = maximumValue;
    }

    public long getTimestampOfLastDisplayedPoint()
    {
        return m_timestampOfLastDisplayedPoint;
    }

    protected void setTimestampOfLastDisplayedPoint(final long timestamp)
    {
        m_timestampOfLastDisplayedPoint = timestamp;
    }

    /**
     * Configure the Y-axis: Adjust the range and specify tick units.
     */
    protected void setRangeAxis(final boolean hideDecimals, final XYPlot plot)
    {
        if (hideDecimals)
        {
            plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        }

        if (getDatasetSize() == 0)
        {
            plot.getRangeAxis().setRange(0.0, 1.0);
        }
        else
        {
            plot.getRangeAxis().setRange(getMinimumValue() - 1, getMaximumValue() + 1);
        }
    }

    private void applyStandardPlotColors(final XYPlot plot)
    {
        plot.setBackgroundPaint(BACK_GROUND_COLOR);
        plot.setRangeGridlinePaint(GRID_LINE_COLOR);
    }

    protected int getDatasetSize()
    {
        return m_datasetSize;
    }
}