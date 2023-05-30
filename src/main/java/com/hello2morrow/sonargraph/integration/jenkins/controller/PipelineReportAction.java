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
import java.io.IOException;
import java.util.logging.Level;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.LatestFolder;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.persistence.TextFileReader;

import hudson.FilePath;
import hudson.model.Action;
import hudson.model.Job;

public class PipelineReportAction implements Action
{

    private final Job<?, ?> job;

    public PipelineReportAction(Job<?, ?> job)
    {
        this.job = job;
    }
    
    public Job<?, ?> getJob()
    {
        return job;
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
        return ConfigParameters.ACTION_URL_PIPELINE_REPORT.getValue();
    }
    
    protected String readHTMLReport(final FilePath pathToReport, String alternative) throws IOException, InterruptedException
    {
        SonargraphLogger.INSTANCE.log(Level.INFO, "Reading Sonargraph HTML Report from '" + pathToReport + "'");

        if (pathToReport.exists())
        {
            return TextFileReader.readLargeTextFile(pathToReport);
        }
        else
        {
            SonargraphLogger.INSTANCE.log(Level.WARNING, "Unable to read Sonargraph HTML report from '" + pathToReport + "'");
            return alternative;
        }
    }

    public String getHTMLReport() throws IOException, InterruptedException
    {
        final File reportFile = LatestFolder.getReport(job);
        return readHTMLReport(new FilePath(reportFile), "Unable to read Sonargraph HTML report.");
    }
}
