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

import jakarta.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.LatestFolder;

import hudson.FilePath;
import hudson.model.Job;

public class InvisibleReportAction extends InvisibleFromSidebarAction
{
    /** Job calling this action. */
    private final Job<?, ?> job;

    public InvisibleReportAction(final Job<?, ?> job)
    {
        this.job = job;
    }

    public Job<?, ?> getJob()
    {
        return job;
    }

    @Override
    public String getUrlName()
    {
        return ConfigParameters.ACTION_URL_REPORT.getValue();
    }

    @Override
    public void doDynamic(final StaplerRequest2 req, final StaplerResponse2 rsp) throws IOException, ServletException
    {
        final File latestFolder =  LatestFolder.getFolder(getJob());
        enableDirectoryBrowserSupport(req, rsp, new FilePath(latestFolder));
    }

    @Override
    public String getHTMLReport() throws IOException, InterruptedException
    {
        final File reportFile = LatestFolder.getReport(job);
        return readHTMLReport(new FilePath(reportFile), "Unable to read Sonargraph HTML report.");
    }
}