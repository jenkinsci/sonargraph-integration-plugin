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

public enum ConfigParameters
{
    ACTION_URL_REPORT("sonargraph-integration-report"),
    ACTION_URL_DIFF("sonargraph-integration-diff"),
    
    ACTION_URL_INTEGRATION("sonargraph-integration"),
    ACTION_URL_PIPELINE_REPORT("sonargraph-pipeline-report"),
    ACTION_URL_PIPELINE_DIFF("sonargraph-pipeline-diff"),
    ACTION_URL_PIPELINE_CHART("sonargraph-pipeline-chart"),
    
    ACTION_DISPLAY_INTEGRATION("Sonargraph Integration"),
    ACTION_DISPLAY_REPORT("Sonargraph Report"),
    ACTION_DISPLAY_DIFF("Sonargraph Diff Report"),
    ACTION_DISPLAY_CHART("Sonargraph Charts"),
    
    REPORT_BUILDER_DISPLAY_NAME("Sonargraph Integration Report Generation & Analysis"),
    SONARGRAPH_ICON("/plugin/sonargraph-integration/icons/Sonargraph.png"),
    JOB_FOLDER("job/"),
    METRIC_HISTORY_CSV_FILE_PATH("sonargraph-integration-metric-history.csv"),
    METRICIDS_HISTORY_JSON_FILE_PATH("sonargraph-integration-metricid-history.json"),
    CHARTS_FOR_METRICS_CSV_FILE_PATH("sonargraph-integration-charts-for-metrics.csv"),
    REPORT_HISTORY_FOLDER("sonargraphIntegrationReportHistory"),
    SONARGRAPH_REPORT_TARGET_DIRECTORY("target/report/"),
    SONARGRAPH_REPORT_FILE_NAME("sonargraph-integration-report"),
    SONARGRAPH_DIFF_FILE_NAME("sonargraph-integration-report_diff");

    private String m_value;

    private ConfigParameters(final String value)
    {
        m_value = value;
    }

    public String getValue()
    {
        return m_value;
    }
}