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

import java.io.File;
import java.io.InputStream;

import com.hello2morrow.sonargraph.integration.access.controller.ControllerFactory;
import com.hello2morrow.sonargraph.integration.access.controller.IMetaDataController;
import com.hello2morrow.sonargraph.integration.access.foundation.ResultWithOutcome;
import com.hello2morrow.sonargraph.integration.access.model.IExportMetaData;
import com.hello2morrow.sonargraph.integration.jenkins.model.IMetricIdsHistoryProvider;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricIds;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricIdsHistory;

import hudson.model.Job;

public class MetricIdProvider
{
    private static final String DEFAULT_META_DATA_XML = "MetaData.xml";
    private static IMetricIdsHistoryProvider s_metricIdsHistory;

    public static ResultWithOutcome<MetricIds> getMetricIds(final Job<?, ?> project)
    {
        final ResultWithOutcome<MetricIds> overallResult = new ResultWithOutcome<>("Get stored MetricIds");
        ResultWithOutcome<MetricIds> historyResult = null;

        // get metricIds from history
        if (s_metricIdsHistory == null && project != null)
        {
            final File metricIdsHistoryFile = new File(project.getParent().getRootDir(),
                    ConfigParameters.METRICIDS_HISTORY_JSON_FILE_PATH.getValue());
            s_metricIdsHistory = new MetricIdsHistory(metricIdsHistoryFile);
        }
        if (s_metricIdsHistory != null)
        {
            historyResult = s_metricIdsHistory.readMetricIds();
            if (historyResult.isFailure())
            {
                overallResult.addMessagesFrom(historyResult);
                return overallResult;
            }
        }

        // get metricIds from export meta data file
        final IMetaDataController controller = ControllerFactory.createMetaDataController();
        final InputStream is = MetricIdProvider.class.getResourceAsStream(DEFAULT_META_DATA_XML);
        final ResultWithOutcome<IExportMetaData> exportMetaDataResult = controller.loadExportMetaData(is, DEFAULT_META_DATA_XML);

        if (exportMetaDataResult.isFailure())
        {
            overallResult.addMessagesFrom(exportMetaDataResult);
            return overallResult;
        }

        // combine and return them
        final MetricIds defaultMetricIds = MetricIds.fromExportMetaData(exportMetaDataResult.getOutcome());
        if (historyResult != null)
        {
            final MetricIds historyMetricIds = historyResult.getOutcome();
            overallResult.setOutcome(defaultMetricIds.addAll(historyMetricIds));
        }
        else
        {
            overallResult.setOutcome(defaultMetricIds);
        }

        return overallResult;
    }
}
