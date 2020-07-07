/*
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2020 hello2morrow GmbH
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
import java.util.logging.Level;

import org.kohsuke.stapler.DataBoundSetter;

import com.hello2morrow.sonargraph.integration.access.model.Severity;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.PluginVersionReader;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.ReportHistoryFileManager;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

public abstract class AbstractSonargraphRecorder extends Recorder
{
    private static final String DEFAULT_ACTION = "nothing";
    
    private String architectureViolationsAction = DEFAULT_ACTION;
    private String unassignedTypesAction = DEFAULT_ACTION;
    private String cyclicElementsAction = DEFAULT_ACTION;
    private String thresholdViolationsAction = DEFAULT_ACTION;
    private String architectureWarningsAction = DEFAULT_ACTION;
    private String workspaceWarningsAction = DEFAULT_ACTION;
    private String workItemsAction = DEFAULT_ACTION;
    private String emptyWorkspaceAction = DEFAULT_ACTION;
    
    public String getArchitectureViolationsAction()
    {
        return architectureViolationsAction;
    }

    public String getUnassignedTypesAction()
    {
        return unassignedTypesAction;
    }

    public String getCyclicElementsAction()
    {
        return cyclicElementsAction;
    }

    public String getThresholdViolationsAction()
    {
        return thresholdViolationsAction;
    }

    public String getArchitectureWarningsAction()
    {
        return architectureWarningsAction;
    }

    public String getWorkspaceWarningsAction()
    {
        return workspaceWarningsAction;
    }

    public String getWorkItemsAction()
    {
        return workItemsAction;
    }

    public String getEmptyWorkspaceAction()
    {
        return emptyWorkspaceAction;
    }

    @DataBoundSetter
    public void setArchitectureViolationsAction(String architectureViolationsAction)
    {
        this.architectureViolationsAction = architectureViolationsAction;
    }

    @DataBoundSetter
    public void setUnassignedTypesAction(String unassignedTypesAction)
    {
        this.unassignedTypesAction = unassignedTypesAction;
    }

    @DataBoundSetter
    public void setCyclicElementsAction(String cyclicElementsAction)
    {
        this.cyclicElementsAction = cyclicElementsAction;
    }

    @DataBoundSetter
    public void setThresholdViolationsAction(String thresholdViolationsAction)
    {
        this.thresholdViolationsAction = thresholdViolationsAction;
    }

    @DataBoundSetter
    public void setArchitectureWarningsAction(String architectureWarningsAction)
    {
        this.architectureWarningsAction = architectureWarningsAction;
    }

    @DataBoundSetter
    public void setWorkspaceWarningsAction(String workspaceWarningsAction)
    {
        this.workspaceWarningsAction = workspaceWarningsAction;
    }

    @DataBoundSetter
    public void setWorkItemsAction(String workItemsAction)
    {
        this.workItemsAction = workItemsAction;
    }

    @DataBoundSetter
    public void setEmptyWorkspaceAction(String emptyWorkspaceAction)
    {
        this.emptyWorkspaceAction = emptyWorkspaceAction;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService()
    {
        return BuildStepMonitor.NONE;
    }

    protected final boolean processSonargraphReport(final Run<?, ?> run, final FilePath sonargraphReportDirectory, final String reportFileName,
            final PrintStream logger) throws IOException, InterruptedException
    {
        assert run != null : "Parameter 'run' of method 'processSonargraphReport' must not be null";
        assert sonargraphReportDirectory != null : "Parameter 'sonargraphReportDirectory' of method 'processSonargraphReport' must not be null";

        final FilePath projectRootDir = new FilePath(run.getParent().getRootDir());
        final ReportHistoryFileManager reportHistoryManager = new ReportHistoryFileManager(projectRootDir,
                ConfigParameters.REPORT_HISTORY_FOLDER.getValue(), ConfigParameters.SONARGRAPH_REPORT_FILE_NAME.getValue(), logger);

        FilePath reportFile = null;
        try
        {
            reportFile = reportHistoryManager.storeGeneratedReportDirectory(sonargraphReportDirectory, reportFileName, run.getNumber(), logger);
        }
        catch (final IOException ex)
        {
            SonargraphLogger.logToConsoleOutput(logger, Level.SEVERE, "Failed to process the generated Sonargraph report", ex);
            return false;
        }

        if (reportFile == null || !reportFile.exists() || reportFile.isRemote() || reportFile.isDirectory())
        {
            SonargraphLogger.logToConsoleOutput(logger, Level.SEVERE, "Sonargraph analysis cannot be executed as Sonargraph report does not exist.",
                    null);
            SonargraphLogger.logToConsoleOutput(logger, Level.SEVERE, "Report file \"" + reportFile + "\" does not exist.", null);
            run.setResult(Result.FAILURE);
            return false;
        }

        final SonargraphBuildAnalyzer sonargraphBuildAnalyzer = new SonargraphBuildAnalyzer(reportFile, logger);

        sonargraphBuildAnalyzer.changeBuildResultIfIssuesExist("ArchitectureViolation", Severity.NONE, architectureViolationsAction);
        sonargraphBuildAnalyzer.changeBuildResultIfMetricValueNotZero("CoreUnassignedComponents", unassignedTypesAction);
        sonargraphBuildAnalyzer.changeBuildResultIfIssuesExist("CycleGroup", Severity.ERROR, cyclicElementsAction);
        sonargraphBuildAnalyzer.changeBuildResultIfIssuesExist("ThresholdViolation", Severity.ERROR, thresholdViolationsAction);
        sonargraphBuildAnalyzer.changeBuildResultIfIssuesExist("ArchitectureConsistency", Severity.NONE, architectureWarningsAction);
        sonargraphBuildAnalyzer.changeBuildResultIfIssuesExist("Workspace", Severity.NONE, workspaceWarningsAction);
        sonargraphBuildAnalyzer.changeBuildResultIfIssuesExist("Todo", Severity.NONE, workItemsAction);
        sonargraphBuildAnalyzer.changeBuildResultIfMetricValueIsZero("CoreComponents", emptyWorkspaceAction);
        final Result buildResult = sonargraphBuildAnalyzer.getOverallBuildResult();

        final File metricHistoryFile = new File(run.getParent().getRootDir(), ConfigParameters.METRIC_HISTORY_CSV_FILE_PATH.getValue());
        final File metricIdsHistoryFile = new File(run.getParent().getRootDir(), ConfigParameters.METRICIDS_HISTORY_JSON_FILE_PATH.getValue());
        try
        {
            sonargraphBuildAnalyzer.saveMetrics(metricHistoryFile, metricIdsHistoryFile, run.getTimestamp().getTimeInMillis(), run.getNumber());
        }
        catch (final IOException ex)
        {
            SonargraphLogger.logToConsoleOutput(logger, Level.SEVERE, "Failed to save Sonargraph metrics to CSV data file", ex);
            return false;
        }
        if (buildResult != null)
        {
            SonargraphLogger.logToConsoleOutput(logger, Level.INFO,
                    "Sonargraph analysis has set the final build result to '" + buildResult.toString() + "'", null);
            run.setResult(buildResult);
        }
        return true;
    }

    protected void logExecutionStart(final AbstractBuild<?, ?> build, final TaskListener listener,
            final Class<? extends AbstractSonargraphRecorder> recorderClazz)
    {
        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO,
                "Sonargraph Jenkins Plugin, Version '" + PluginVersionReader.INSTANCE.getVersion() + "', post-build step '" + recorderClazz.getName()
                        + "'\n" + "Start structural analysis on project '" + build.getProject().getDisplayName() + "', build number '"
                        + build.getNumber() + "'",
                null);
    }

    protected void logExecutionStart(final Run<?, ?> run, final TaskListener listener,
            final Class<? extends AbstractSonargraphRecorder> recorderClazz)
    {
        SonargraphLogger.logToConsoleOutput(listener.getLogger(), Level.INFO,
                "Sonargraph Jenkins Plugin, Version '" + PluginVersionReader.INSTANCE.getVersion() + "', post-build step '" + recorderClazz.getName()
                        + "'\n" + "Start structural analysis on project '" + run.getParent().getDisplayName() + "', build number '" + run.getNumber()
                        + "'",
                null);
    }
}
