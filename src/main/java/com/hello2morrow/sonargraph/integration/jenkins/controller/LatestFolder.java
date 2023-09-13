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

import hudson.model.Job;

final class LatestFolder
{
    private LatestFolder()
    {
        super();
    }

    static File getFolder(Job<?, ?> job)
    {
        final File jobRootFolder = job.getRootDir();
        final File reportHistoryFolder = new File(jobRootFolder, ConfigParameters.REPORT_HISTORY_FOLDER.getValue());
        final File latestFolder = new File(reportHistoryFolder, "latest");
        return latestFolder;
    }

    static File getReport(Job<?, ?> job)
    {
        final File latestFolder = getFolder(job);
        final String reportFileName = ConfigParameters.SONARGRAPH_REPORT_FILE_NAME.getValue() + ".html";
        final File reportFile = new File(latestFolder, reportFileName);
        return reportFile;
    }

    static boolean hasReport(Job<?, ?> job)
    {
        final File reportFile = getReport(job);
        return reportFile.exists() && reportFile.isFile() && reportFile.canRead();
    }

    static File getDiffReport(Job<?, ?> job)
    {
        final File latestFolder = getFolder(job);
        final String reportFileName = ConfigParameters.SONARGRAPH_DIFF_FILE_NAME.getValue() + ".html";
        final File reportFile = new File(latestFolder, reportFileName);
        return reportFile;
    }

    static boolean hasDiffReport(Job<?, ?> job)
    {
        final File reportFile = getDiffReport(job);
        return reportFile.exists() && reportFile.isFile() && reportFile.canRead();
    }
}