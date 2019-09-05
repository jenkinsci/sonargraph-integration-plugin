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
package com.hello2morrow.sonargraph.integration.jenkins.controller;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.FilePath;
import hudson.model.AbstractProject;

public final class SonargraphHTMLReportAction extends InvisibleFromSidebarAction
{
    /** Project or build that is calling this action. */
    private final AbstractProject<?, ?> project;

    public SonargraphHTMLReportAction(final AbstractProject<?, ?> project)
    {
        this.project = project;
    }

    public AbstractProject<?, ?> getProject()
    {
        return project;
    }

    @Override
    public String getUrlName()
    {
        return ConfigParameters.HTML_REPORT_ACTION_URL.getValue();
    }

    @Override
    public void doDynamic(final StaplerRequest req, final StaplerResponse rsp) throws IOException, ServletException
    {
        final File latestFolder = getLatestFolder();
        enableDirectoryBrowserSupport(req, rsp, new FilePath(latestFolder));
    }

    private File getLatestFolder()
    {
        final File projectRootFolder = project.getRootDir();
        final File reportHistoryFolder = new File(projectRootFolder, ConfigParameters.REPORT_HISTORY_FOLDER.getValue());
        final File latestFolder = new File(reportHistoryFolder, "latest");
        return latestFolder;
    }

    @Override
    public String getHTMLReport() throws IOException, InterruptedException
    {
        final File latestFolder = getLatestFolder();
        final String reportFileName = ConfigParameters.SONARGRAPH_HTML_REPORT_FILE_NAME.getValue() + ".html";
        final File reportFile = new File(latestFolder, reportFileName);
        return readHTMLReport(new FilePath(reportFile));
    }
}