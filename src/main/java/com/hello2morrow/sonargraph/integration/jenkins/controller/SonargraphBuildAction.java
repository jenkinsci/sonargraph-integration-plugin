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
import hudson.model.AbstractBuild;

public final class SonargraphBuildAction extends AbstractHTMLAction
{
    private final AbstractBuild<?, ?> build;

    public SonargraphBuildAction(final AbstractBuild<?, ?> build)
    {
        this.build = build;
    }

    @Override
    public void doDynamic(final StaplerRequest req, final StaplerResponse rsp) throws IOException, ServletException
    {
        final FilePath reportHistoryDir = new FilePath(new FilePath(build.getProject().getRootDir()),
                ConfigParameters.REPORT_HISTORY_FOLDER.getValue());
        enableDirectoryBrowserSupport(req, rsp, new FilePath(reportHistoryDir, "sonargraph-report-build-" + build.getNumber()));
    }

    public AbstractBuild<?, ?> getBuild()
    {
        return build;
    }

    @Override
    public String getIconFileName()
    {
        return ConfigParameters.SONARGRAPH_ICON.getValue();
    }

    @Override
    public String getDisplayName()
    {
        return ConfigParameters.ACTION_DISPLAY_NAME.getValue();
    }

    @Override
    public String getUrlName()
    {
        return ConfigParameters.HTML_REPORT_ACTION_URL.getValue();
    }

    @Override
    public String getHTMLReport() throws IOException, InterruptedException
    {
        final File projectRootFolder = build.getProject().getRootDir();
        final File reportHistoryFolder = new File(projectRootFolder, ConfigParameters.REPORT_HISTORY_FOLDER.getValue());
        final File reportBuildFolder = new File(reportHistoryFolder, "sonargraph-report-build-" + build.getNumber());
        final String reportFileName = ConfigParameters.SONARGRAPH_HTML_REPORT_FILE_NAME.getValue() + ".html";
        final File reportFile = new File(reportBuildFolder, reportFileName);
        return readHTMLReport(new FilePath(reportFile));
    }
}