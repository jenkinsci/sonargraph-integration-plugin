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
package com.hello2morrow.sonargraph.integration.jenkins.persistence;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;

import com.hello2morrow.sonargraph.integration.jenkins.controller.ConfigParameters;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;

import hudson.FilePath;

/**
 * Class that handles copies of each generated architect report to calculate trends or
 * generate graphics.
 * 
 * @author esteban
 * @author andreas
 *
 */
public final class ReportHistoryFileManager
{

    /** Path to the folder containing sonargraph report files generated for every build */
    private final FilePath m_sonargraphReportHistoryDir;

    public ReportHistoryFileManager(final FilePath projectRootDir, final String reportHistoryBaseDirectoryName, final PrintStream logger)
            throws IOException, InterruptedException
    {
        assert projectRootDir != null : "The path to the folder where architect reports are stored must not be null";
        assert !projectRootDir.isRemote() : "The path to the folder where architect reports are stored must not be remote";
        assert reportHistoryBaseDirectoryName != null
                && !reportHistoryBaseDirectoryName.isEmpty() : "reportHistoryBaseDirectoryName must not be empty";
        assert logger != null : "Parameter 'logger' of method 'ReportHistoryFileManager' must not be null";

        m_sonargraphReportHistoryDir = new FilePath(projectRootDir, reportHistoryBaseDirectoryName);
        if (!m_sonargraphReportHistoryDir.exists())
        {
            try
            {
                m_sonargraphReportHistoryDir.mkdirs();
            }
            catch (final IOException ex)
            {
                SonargraphLogger.logToConsoleOutput(logger, Level.SEVERE,
                        "Failed to create directory '" + m_sonargraphReportHistoryDir.getRemote() + "'", ex);
            }
        }
    }

    public FilePath getReportHistoryDirectory()
    {
        return m_sonargraphReportHistoryDir;
    }

    public FilePath storeGeneratedReportDirectory(final FilePath reportDirectory, final String reportName, final Integer buildNumber, final PrintStream logger)
            throws IOException, InterruptedException
    {
        assert reportDirectory != null : "Parameter 'reportDirectory' of method 'soterdGeneratedReportDirectory' must not be null";
        assert reportDirectory.exists() : "Parameter 'reportDirectory' must be an existing folder. '" + reportDirectory.getRemote()
                + "' does not exist.";

        if (!m_sonargraphReportHistoryDir.exists())
        {
            final String msg = "Unable to create directory " + m_sonargraphReportHistoryDir.getRemote();
            SonargraphLogger.logToConsoleOutput(logger, Level.SEVERE, msg, null);
            throw new IOException(msg);
        }
        // copy all report related files (*.gif, *.css, ...) except xml and html report
        final FilePath targetHistoryDirectory = new FilePath(m_sonargraphReportHistoryDir, "sonargraph-report-build-" + buildNumber);
        reportDirectory.copyRecursiveTo("**/*.*", "*.html,*.xml", targetHistoryDirectory);
        SonargraphLogger.logToConsoleOutput(logger, Level.INFO, "Copied report related files to directory " + targetHistoryDirectory.getRemote(),
                null);

        // copy xml report, and rename it
        FilePath targetXmlReportFile = null;
        final FilePath sourceXmlReportFile = new FilePath(reportDirectory, reportName + ".xml");
        if (sourceXmlReportFile.exists())
        {
            targetXmlReportFile = new FilePath(targetHistoryDirectory, ConfigParameters.SONARGRAPH_REPORT_FILE_NAME.getValue() + ".xml");
            sourceXmlReportFile.copyTo(targetXmlReportFile);
            SonargraphLogger.logToConsoleOutput(logger, Level.INFO,
                    "Copied xml report file from " + sourceXmlReportFile.getRemote() + " to " + targetXmlReportFile.getRemote(), null);
        }
        else
        {
            SonargraphLogger.logToConsoleOutput(logger, Level.WARNING, "No xml report file found at " + sourceXmlReportFile.getRemote(), null);
        }

        // copy html report, and rename it
        final FilePath sourceHtmlReportFile = new FilePath(reportDirectory, reportName + ".html");
        if (sourceHtmlReportFile.exists())
        {
            final FilePath targetHtmlReportFile = new FilePath(targetHistoryDirectory,
                    ConfigParameters.SONARGRAPH_REPORT_FILE_NAME.getValue() + ".html");
            sourceHtmlReportFile.copyTo(targetHtmlReportFile);
            SonargraphLogger.logToConsoleOutput(logger, Level.INFO,
                    "Copied html report file from " + sourceHtmlReportFile.getRemote() + " to " + targetHtmlReportFile.getRemote(), null);
        }
        else
        {
            SonargraphLogger.logToConsoleOutput(logger, Level.WARNING, "No html report file found at " + sourceHtmlReportFile.getRemote(), null);
        }

        // return copied xml report from master
        return targetXmlReportFile;
    }
}
