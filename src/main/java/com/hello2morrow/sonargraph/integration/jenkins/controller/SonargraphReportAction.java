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
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.tasks.SimpleBuildStep;

public final class SonargraphReportAction extends AbstractHTMLAction implements SimpleBuildStep.LastBuildAction
{
    private final Run<?, ?> run;

    public SonargraphReportAction(final Run<?, ?> run)
    {
        this.run = run;
    }

    @Override
    public void doDynamic(final StaplerRequest req, final StaplerResponse rsp) throws IOException, ServletException
    {
        final FilePath reportHistoryDir = new FilePath(new FilePath(run.getParent().getRootDir()), ConfigParameters.REPORT_HISTORY_FOLDER.getValue());
        enableDirectoryBrowserSupport(req, rsp, new FilePath(reportHistoryDir, "sonargraph-report-build-" + run.getNumber()));
    }

    public Run<?, ?> getRun()
    {
        return run;
    }

    @Override
    public String getIconFileName()
    {
        return ConfigParameters.SONARGRAPH_ICON.getValue();
    }

    @Override
    public String getDisplayName()
    {
        return ConfigParameters.ACTION_DISPLAY_REPORT.getValue();
    }

    @Override
    public String getUrlName()
    {
        return ConfigParameters.ACTION_URL_REPORT.getValue();
    }

    @Override
    public String getHTMLReport() throws IOException, InterruptedException
    {
        final File projectRootFolder = run.getParent().getRootDir();
        final File reportHistoryFolder = new File(projectRootFolder, ConfigParameters.REPORT_HISTORY_FOLDER.getValue());
        final File reportBuildFolder = new File(reportHistoryFolder, "sonargraph-report-build-" + run.getNumber());
        final String reportFileName = ConfigParameters.SONARGRAPH_REPORT_FILE_NAME.getValue() + ".html";
        final File reportFile = new File(reportBuildFolder, reportFileName);
        return readHTMLReport(new FilePath(reportFile), "Unable to read Sonargraph HTML report.");
    }

    @Override
    public Collection<Action> getProjectActions()
    {
        final Collection<Action> result = new ArrayList<>();
        final Job<?, ?> job = getRun().getParent();
        if (JobCategory.isPipelineJob(job))
        {
            result.add(PipelineChartAction.getInstance(job));
            result.add(new PipelineReportAction(job));
            PipelineDiffAction diffAction = new PipelineDiffAction(job);
            if (diffAction.isPossible())
            {
                result.add(diffAction);
            }
        }
        result.add(new InvisibleReportAction(job));
        result.add(new InvisibleDiffAction(job));
        return result;
    }
}