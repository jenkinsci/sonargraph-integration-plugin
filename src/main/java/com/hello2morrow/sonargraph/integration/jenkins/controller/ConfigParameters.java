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

public enum ConfigParameters
{
    ACTION_URL_NAME("sonargraphIntegration"),
    SONARGRAPH_ICON("/plugin/sonargraph-integration/icons/Sonargraph.png"),
    ACTION_DISPLAY_NAME("Sonargraph Integration"),
    REPORT_BUILDER_DISPLAY_NAME("Sonargraph Integration Report Generation & Analysis"),
    JOB_FOLDER("job/"),
    HTML_REPORT_ACTION_URL("sonargraph-integration-html-report"),
    METRIC_HISTORY_CSV_FILE_PATH("sonargraph-integration-metric-history.csv"),
    METRICIDS_HISTORY_JSON_FILE_PATH("sonargraph-integration-metricid-history.json"),
    CHARTS_FOR_METRICS_CSV_FILE_PATH("sonargraph-integration-charts-for-metrics.csv"),
    SONARGRAPH_HTML_REPORT_FILE_NAME("sonargraph-integration-report"),
    REPORT_HISTORY_FOLDER("sonargraphIntegrationReportHistory"),
    SONARGRAPH_REPORT_TARGET_DIRECTORY("target/report/"),
    SONARGRAPH_REPORT_FILE_NAME("sonargraph-integration-report");

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