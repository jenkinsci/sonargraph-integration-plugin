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
package com.hello2morrow.sonargraph.integration.jenkins.controller;

import hudson.FilePath;
import hudson.model.AbstractProject;

import java.io.IOException;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

public final class SonargraphHTMLReportAction extends InvisibleFromSidebarAction
{
    /** Project or build that is calling this action. */
    private final AbstractProject<?, ?> project;

    /** Object that defines the post-build step associated with this action. */
    private final IReportPathProvider pathProvider;

    public SonargraphHTMLReportAction(final AbstractProject<?, ?> project, final IReportPathProvider pathProvider)
    {
        this.project = project;
        this.pathProvider = pathProvider;
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
        final FilePath directory = new FilePath(project.getSomeWorkspace(), pathProvider.getReportDirectory());
        enableDirectoryBrowserSupport(req, rsp, directory);
    }

    @Override
    public String getHTMLReport() throws IOException, InterruptedException
    {
        final String reportRelativePath = pathProvider.getReportPath() + ".html";
        final FilePath reportAbsouletPath = new FilePath(project.getSomeWorkspace(), reportRelativePath);

        return readHTMLReport(reportAbsouletPath);
    }
}