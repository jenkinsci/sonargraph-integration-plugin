/*******************************************************************************
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2016 hello2morrow GmbH
 * mailto: info AT hello2morrow DOT com
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
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *******************************************************************************/
package com.hello2morrow.sonargraph.integration.jenkins.model;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

public class XYLineAndShapePlot extends AbstractPlot
{
    private static final String COLON = ": ";
    private static final String BUILD = "Build #";

    public XYLineAndShapePlot(IMetricHistoryProvider datasetProvider)
    {
        super(datasetProvider);
    }

    @Override
    protected JFreeChart createChartInternal(String chartTitle, String categoryName, String yAxisName, XYDataset dataset)
    {
        return ChartFactory.createXYLineChart(chartTitle, categoryName, yAxisName, dataset, PlotOrientation.VERTICAL, false, true, false);
    }

    @Override
    protected void applyRendering(XYPlot plot)
    {
        NumberAxis axis = (NumberAxis) plot.getDomainAxis();
        axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseShapesVisible(true);
        renderer.setSeriesPaint(0, DATA_COLOR);

        //Unfortunately, the tooltips are not visible, when the graph gets rendered as a PNG
        StandardXYToolTipGenerator toolTipGenerator = new StandardXYToolTipGenerator()
        {
            private static final long serialVersionUID = -5803780142385784897L;

            @Override
            public String generateToolTip(XYDataset dataset, int series, int item)
            {
                return new StringBuilder(BUILD).append(dataset.getXValue(series, item)).append(COLON).append(dataset.getYValue(series, item))
                        .toString();
            }
        };
        renderer.setBaseToolTipGenerator(toolTipGenerator);
    }
}
