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

import java.awt.Color;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricId;

public class TimeSeriesPlot extends AbstractPlot
{
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US); // NOSONAR

    private static final int MOVING_AVG_PERIOD = 1000 * 60 * 60 * 24 * 1;
    private int m_markerPosition = 0;
    private long m_markerTimestamp = 0;

    public TimeSeriesPlot(final IMetricHistoryProvider dataProvider, final int markerPosition)
    {
        super(dataProvider);
        m_markerPosition = markerPosition;
    }

    @Override
    protected JFreeChart createChartInternal(final String chartTitle, final String categoryName, final String yAxisName, final XYDataset dataset)
    {
        return ChartFactory.createTimeSeriesChart(chartTitle, categoryName, yAxisName, dataset, false, false, false);
    }

    /**
     * Creates a XYDataset from a CSV file.
     */
    @Override
    protected XYDataset createXYDataset(final MetricId metric, final int maximumNumberOfDataPoints) throws IOException
    {
        assert metric != null : "Parameter 'metric' of method 'createXYDataset' must not be null";

        //For some reason, the class of the time series is required here, otherwise an exception is thrown that a Date instance is expected.
        @SuppressWarnings("deprecation")
        final TimeSeries timeSeries = new TimeSeries(metric.getName(), FixedMillisecond.class);

        final List<IDataPoint> dataset = m_datasetProvider.readMetricValues(metric);
        final int size = dataset.size();
        SonargraphLogger.INSTANCE
                .fine(size + " data points found for metric '" + metric.getName() + "' in file '" + m_datasetProvider.getStorageName() + "'");
        final List<IDataPoint> reducedSet = reduceDataSet(dataset, maximumNumberOfDataPoints);

        BuildDataPoint point = null;
        for (final IDataPoint datapoint : reducedSet)
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
                if (point.getTimestamp() == 0)
                {
                    continue;
                }

                timeSeries.add(new FixedMillisecond(point.getTimestamp()), point.getY());
            }
        }
        if (point != null)
        {
            setTimestampOfLastDisplayedPoint(point.getTimestamp());
        }

        final TimeSeriesCollection timeSeriesCollection = new TimeSeriesCollection();
        final TimeSeries avgDataset = MovingAverage.createMovingAverage(timeSeries, "Avg of " + metric.getName(), MOVING_AVG_PERIOD, 0);
        setDataSetSize(avgDataset.getItemCount());
        timeSeriesCollection.addSeries(avgDataset);

        //SG-325: We cannot use JFreeChart methods of version 1.0.14
        //        setMinimumValue(avgDataset.getMinY());
        //        setMaximumValue(avgDataset.getMaxY());

        // We only show the average data and omit the original data
        //        timeSeriesCollection.addSeries(timeSeries);
        for (final Object item : avgDataset.getItems())
        {
            if (item instanceof TimeSeriesDataItem)
            {
                checkMinMaxYValue(((TimeSeriesDataItem) item).getValue().doubleValue());
            }
        }
        return timeSeriesCollection;
    }

    /**
     *
     * @param maxDataPoints don't reduce the set if  maxDataPoints <= 0
     */
    private List<IDataPoint> reduceDataSet(final List<IDataPoint> dataset, final int maxDataPoints)
    {
        assert dataset != null : "Parameter 'dataset' of method 'reduceDataSet' must not be null";
        if (maxDataPoints <= 0)
        {
            return dataset;
        }
        final int size = dataset.size();
        if (size <= maxDataPoints)
        {
            if (size > m_markerPosition)
            {
                final IDataPoint point = dataset.get(dataset.size() - m_markerPosition);
                if (point instanceof BuildDataPoint)
                {
                    m_markerTimestamp = ((BuildDataPoint) point).getTimestamp();
                }
            }
            return dataset;
        }

        int compressionFactor;

        if ((size % maxDataPoints) == 0)
        {
            compressionFactor = size / maxDataPoints;
        }
        else
        {
            compressionFactor = (size / maxDataPoints) + 1;
        }

        final List<IDataPoint> compressedSet = new ArrayList<>();
        SonargraphLogger.INSTANCE.log(Level.FINE, "Compressing data set of size '" + size + "' by a factor of '" + compressionFactor + "'");
        for (int i = 0; i < size; i = i + compressionFactor)
        {
            double valueSum = 0.0;
            long timestamp = 0L;
            int buildNumber = 0;
            int actualFactor = 0;
            for (int j = 0; j < compressionFactor; j++)
            {
                if ((i + j) >= size)
                {
                    break;
                }
                actualFactor = j + 1;
                final IDataPoint point = dataset.get(i + j);
                if (point instanceof BuildDataPoint)
                {
                    valueSum += point.getY();
                    buildNumber = point.getX();
                    timestamp = ((BuildDataPoint) point).getTimestamp();
                    if ((i + j) == (size - m_markerPosition))
                    {
                        m_markerTimestamp = timestamp;
                    }
                }
                else
                {
                    SonargraphLogger.INSTANCE.log(Level.FINE, "DataPoint [" + (i + j) + "] is of type '" + point.getClass().getName()
                            + "', expect type '" + BuildDataPoint.class.getName() + "'");
                }
            }
            compressedSet.add(new BuildDataPoint(buildNumber, valueSum / actualFactor, timestamp));
        }
        return compressedSet;
    }

    @Override
    protected void applyRendering(final XYPlot plot)
    {
        final DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(DATE_FORMAT);

        final XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseShapesVisible(false);
        renderer.setSeriesPaint(0, DATA_COLOR);

        if (m_markerTimestamp > 0)
        {
            final Marker target = new ValueMarker(m_markerTimestamp);
            target.setPaint(Color.RED);
            target.setLabel("Short Term");
            if ((m_markerPosition * 2) > getDatasetSize())
            {
                //Move the label to the left of the marker
                target.setLabelAnchor(RectangleAnchor.TOP_RIGHT);
                target.setLabelTextAnchor(TextAnchor.TOP_LEFT);
            }
            else
            {
                target.setLabelAnchor(RectangleAnchor.TOP_LEFT);
                target.setLabelTextAnchor(TextAnchor.TOP_RIGHT);
            }
            plot.addDomainMarker(target);
        }
    }
}
