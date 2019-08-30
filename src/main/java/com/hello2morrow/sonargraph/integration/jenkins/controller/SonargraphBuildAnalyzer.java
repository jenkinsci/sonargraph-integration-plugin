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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;

import org.jenkinsci.remoting.RoleChecker;

import com.hello2morrow.sonargraph.integration.access.controller.ControllerFactory;
import com.hello2morrow.sonargraph.integration.access.controller.ISonargraphSystemController;
import com.hello2morrow.sonargraph.integration.access.controller.ISystemInfoProcessor;
import com.hello2morrow.sonargraph.integration.access.foundation.Result;
import com.hello2morrow.sonargraph.integration.access.model.IIssue;
import com.hello2morrow.sonargraph.integration.access.model.IMetricId;
import com.hello2morrow.sonargraph.integration.access.model.IMetricValue;
import com.hello2morrow.sonargraph.integration.access.model.Severity;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.model.IMetricHistoryProvider;
import com.hello2morrow.sonargraph.integration.jenkins.model.IMetricIdsHistoryProvider;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.CSVFileHandler;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricId;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricIds;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricIdsHistory;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;

/**
 * Class that analyzes the values found for the metrics and takes action depending of what the user selected to do.
 *
 * @author andreashoyerh2m
 */
class SonargraphBuildAnalyzer
{
    /**
     * HashMap containing a code for the build result and a Result object for each code.
     */
    private final HashMap<String, hudson.model.Result> m_buildResults = new HashMap<>();

    /** Final result of the build process after being affected by the metrics analysis.s */
    private hudson.model.Result m_overallBuildResult;

    private PrintStream m_logger = null;
    private final ISonargraphSystemController m_controller = ControllerFactory.createController();

    /**
     * Constructor.
     *
     * @param sonargraphReportPath Absolute path to the Sonargraph report.
     * @throws InterruptedException
     * @throws IOException
     */
    public SonargraphBuildAnalyzer(final FilePath sonargraphReportPath, final PrintStream logger) throws IOException, InterruptedException
    {
        assert sonargraphReportPath != null : "The path for the Sonargraph architect report must not be null";

        assert logger != null : "Parameter 'logger' of method 'SonargraphBuildAnalyzer' mu st not be null";
        m_logger = logger;

        final Result operationResult = sonargraphReportPath.act(new LoadSystemReport(m_controller));

        if (operationResult.isFailure())
        {
            SonargraphLogger.logToConsoleOutput(m_logger, Level.SEVERE,
                    "Failed to load report from '" + sonargraphReportPath + "': " + operationResult.toString(), null);
        }

        m_buildResults.put(BuildActionsEnum.UNSTABLE.getActionCode(), hudson.model.Result.UNSTABLE);
        m_buildResults.put(BuildActionsEnum.FAILED.getActionCode(), hudson.model.Result.FAILURE);

        m_overallBuildResult = null;
    }

    /**
     * Analyzes architecture specific metrics.
     */
    public hudson.model.Result changeBuildResultIfViolationThresholdsExceeded(final Integer thresholdUnstable, final Integer thresholdFailed)
    {
        if (!m_controller.hasSoftwareSystem())
        {
            return null;
        }

        final ISystemInfoProcessor infoProcessor = m_controller.createSystemInfoProcessor();
        final Predicate<IIssue> filter = (final IIssue issue) -> issue.getIssueType().getCategory().getName().equals("ArchitectureViolation")
                && !issue.hasResolution();
        final Integer numberOfViolations = infoProcessor.getIssues(filter).size();
        if (numberOfViolations > 0)
        {
            if (numberOfViolations >= thresholdFailed)
            {
                return m_buildResults.get(BuildActionsEnum.FAILED.getActionCode());
            }

            if ((numberOfViolations >= thresholdUnstable) && (numberOfViolations < thresholdFailed))
            {
                return m_buildResults.get(BuildActionsEnum.UNSTABLE.getActionCode());
            }
        }
        return null;
    }

    public void changeBuildResultIfIssuesExist(final String issueCategory, final Severity minimumSeverity, final String userDefinedAction)
    {
        if (!m_controller.hasSoftwareSystem())
        {
            return;
        }

        final ISystemInfoProcessor infoProcessor = m_controller.createSystemInfoProcessor();
        final Predicate<IIssue> filter = (final IIssue issue) -> issue.getIssueType().getCategory().getName().equals(issueCategory)
                && issue.getIssueType().getSeverity().ordinal() <= minimumSeverity.ordinal() && !issue.hasResolution();
        final int numberOfIssues = infoProcessor.getIssues(filter).size();
        if (numberOfIssues <= 0)
        {
            SonargraphLogger.logToConsoleOutput(m_logger, Level.FINE,
                    "Not changing build result because number of '" + issueCategory + "' is " + numberOfIssues, null);
            return;
        }

        if (userDefinedAction.equals(BuildActionsEnum.FAILED.getActionCode()))
        {
            m_overallBuildResult = m_buildResults.get(BuildActionsEnum.FAILED.getActionCode());
            SonargraphLogger.logToConsoleOutput(m_logger, Level.INFO,
                    "Changing build result to " + m_overallBuildResult.toString() + " because value for '" + issueCategory + "' is " + numberOfIssues,
                    null);
        }
        else if (userDefinedAction.equals(BuildActionsEnum.UNSTABLE.getActionCode())
                && ((m_overallBuildResult == null) || !m_overallBuildResult.equals(hudson.model.Result.FAILURE)))
        {
            m_overallBuildResult = m_buildResults.get(BuildActionsEnum.UNSTABLE.getActionCode());
            SonargraphLogger.logToConsoleOutput(m_logger, Level.INFO,
                    "Changing build result to " + m_overallBuildResult.toString() + " because value for '" + issueCategory + "' is " + numberOfIssues,
                    null);
        }
    }

    public void changeBuildResultIfMetricValueNotZero(final String metricName, final String userDefinedAction)
    {
        if (!m_controller.hasSoftwareSystem())
        {
            return;
        }

        final ISystemInfoProcessor infoProcessor = m_controller.createSystemInfoProcessor();
        final Optional<IMetricValue> metricValue = infoProcessor.getMetricValue(metricName);
        if (!metricValue.isPresent())
        {
            SonargraphLogger.logToConsoleOutput(m_logger, Level.WARNING, "Metric '" + metricName + "' not present in analysis", null);
            return;
        }

        final int value = metricValue.get().getValue().intValue();
        if (value <= 0)
        {
            return;
        }

        if (userDefinedAction.equals(BuildActionsEnum.FAILED.getActionCode()))
        {
            m_overallBuildResult = m_buildResults.get(BuildActionsEnum.FAILED.getActionCode());
            SonargraphLogger.logToConsoleOutput(m_logger, Level.INFO, "Changing build result to " + m_overallBuildResult.toString()
                    + " because value for '" + metricValue.get().getId().getPresentationName() + "' is " + value, null);
        }
        else if (userDefinedAction.equals(BuildActionsEnum.UNSTABLE.getActionCode())
                && ((m_overallBuildResult == null) || !m_overallBuildResult.equals(hudson.model.Result.FAILURE)))
        {
            m_overallBuildResult = m_buildResults.get(BuildActionsEnum.UNSTABLE.getActionCode());
            SonargraphLogger.logToConsoleOutput(m_logger, Level.INFO, "Changing build result to " + m_overallBuildResult.toString()
                    + " because value for '" + metricValue.get().getId().getPresentationName() + "' is " + value, null);
        }
    }

    public void changeBuildResultIfMetricValueIsZero(final String metricName, final String userDefinedAction)
    {
        if (!m_controller.hasSoftwareSystem())
        {
            return;
        }
        final ISystemInfoProcessor infoProcessor = m_controller.createSystemInfoProcessor();
        final Optional<IMetricValue> metricValue = infoProcessor.getMetricValue(metricName);

        if (!metricValue.isPresent())
        {
            SonargraphLogger.logToConsoleOutput(m_logger, Level.WARNING, "Metric '" + metricName + "' not present in analysis", null);
            return;
        }

        final int value = metricValue.get().getValue().intValue();
        if (value == 0)
        {
            if (userDefinedAction.equals(BuildActionsEnum.FAILED.getActionCode()))
            {
                m_overallBuildResult = m_buildResults.get(BuildActionsEnum.FAILED.getActionCode());
                SonargraphLogger.logToConsoleOutput(m_logger, Level.INFO, "Changing build result to " + m_overallBuildResult.toString()
                        + " because value for " + metricValue.get().getId().getName() + " is " + value, null);

            }
            else if (userDefinedAction.equals(BuildActionsEnum.UNSTABLE.getActionCode())
                    && ((m_overallBuildResult == null) || !m_overallBuildResult.equals(hudson.model.Result.FAILURE)))
            {
                m_overallBuildResult = m_buildResults.get(BuildActionsEnum.UNSTABLE.getActionCode());
                SonargraphLogger.logToConsoleOutput(m_logger, Level.INFO, "Changing build result to " + m_overallBuildResult.toString()
                        + " because value for " + metricValue.get().getId().getName() + " is " + value, null);
            }
        }
        else
        {
            SonargraphLogger.logToConsoleOutput(m_logger, Level.FINE,
                    "Not changing build result because value for '" + metricValue.get().getId().getName() + "' is " + value, null);

        }

    }

    /**
     * Append all metric values from report to sonargraph history file. Append all metricIds from report to sonargraph history file.
     */
    public void saveMetrics(final File metricHistoryFile, final File metricIdsHistoryFile, final long timeOfBuild, final Integer buildNumber)
            throws IOException
    {
        SonargraphLogger.logToConsoleOutput(m_logger, Level.INFO, "Save metrics to history", null);
        if (!m_controller.hasSoftwareSystem())
        {
            SonargraphLogger.INSTANCE.log(Level.WARNING, "Software System expected");
            return;
        }

        final ISystemInfoProcessor infoProcessor = m_controller.createSystemInfoProcessor();

        final IMetricIdsHistoryProvider metricIdsHistory = new MetricIdsHistory(metricIdsHistoryFile);
        final MetricIds metricIds = metricIdsHistory.addMetricIds(MetricIds.fromIMetricIds(infoProcessor.getMetricIds()), m_logger);

        final IMetricHistoryProvider metricHistory = new CSVFileHandler(metricHistoryFile, metricIds);
        final HashMap<MetricId, String> buildMetricValues = new HashMap<>();
        for (final IMetricId metric : infoProcessor.getMetricIds())
        {
            final Optional<IMetricValue> systemMetricValue = infoProcessor.getMetricValue(metric.getName());
            if (systemMetricValue.isPresent())
            {
                buildMetricValues.put(MetricId.from(metric), systemMetricValue.get().getValue().toString());
            }
        }

        metricHistory.writeMetricValues(buildNumber, timeOfBuild, buildMetricValues);
    }

    public hudson.model.Result getOverallBuildResult()
    {
        return m_overallBuildResult;
    }

    private static final class LoadSystemReport implements FilePath.FileCallable<Result>
    {
        private static final long serialVersionUID = 2405830264590692887L;

        private final ISonargraphSystemController m_controller;

        public LoadSystemReport(final ISonargraphSystemController controller)
        {
            m_controller = controller;
        }

        @Override
        public void checkRoles(final RoleChecker checker) throws SecurityException
        {
            // do nothing
        }

        @Override
        public Result invoke(final File file, final VirtualChannel channel) throws IOException, InterruptedException
        {
            assert file != null : "Parameter 'file' in method 'invoke' must not be null";
            SonargraphLogger.INSTANCE.log(Level.INFO, "Load system report from file " + file.getAbsolutePath());
            return m_controller.loadSystemReport(file);
        }
    }
}
