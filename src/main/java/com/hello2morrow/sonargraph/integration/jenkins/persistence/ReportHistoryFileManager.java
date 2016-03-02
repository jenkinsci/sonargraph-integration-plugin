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
package com.hello2morrow.sonargraph.integration.jenkins.persistence;

import hudson.FilePath;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;

/**
 * Class that handles copies of each generated architect report to calculate trends or
 * generate graphics.
 * @author esteban
 *
 */
public class ReportHistoryFileManager
{
    public static final String SONARGRAPH_JENKINS_REPORT_FILE_NAME_PREFIX = "sonargraph-jenkins-report-build-";

    /** Path to the folder containing sonargraph report files generated for every build */
    private FilePath m_sonargraphReportHistoryDir;

    public ReportHistoryFileManager(FilePath projectRootDir, String reportHistoryDirName, PrintStream logger) throws IOException,
            InterruptedException
    {
        assert projectRootDir != null : "The path to the folder where architect reports are stored must not be null";
        assert logger != null : "Parameter 'logger' of method 'ReportHistoryFileManager' must not be null";

        m_sonargraphReportHistoryDir = new FilePath(projectRootDir, reportHistoryDirName);
        if (!m_sonargraphReportHistoryDir.exists())
        {
            try
            {
                m_sonargraphReportHistoryDir.mkdirs();
            }
            catch (IOException ex)
            {
                SonargraphLogger.logToConsoleOutput(logger, Level.SEVERE, "Failed to create directory '" + m_sonargraphReportHistoryDir.getRemote()
                        + "'", ex);
            }
        }
    }

    public FilePath getReportHistoryDirectory()
    {
        return m_sonargraphReportHistoryDir;
    }

    /**
     * Stores a generated architect report in the location defined for this purpose. 
     * @param architectReport the architect report file.
     * @throws InterruptedException 
     */
    public void storeGeneratedReport(FilePath architectReport, Integer buildNumber, PrintStream logger) throws IOException, InterruptedException
    {
        assert architectReport != null : "Parameter 'architectReport' of method 'storeGeneratedReport' must not be null";
        assert architectReport.exists() : "Parameter 'architectReport' must be an existing file. '" + architectReport.getRemote()
                + "' does not exist.";

        if ((architectReport == null) || (buildNumber == null))
        {
            return;
        }

        if (!m_sonargraphReportHistoryDir.exists())
        {
            String msg = "Unable to create directory " + m_sonargraphReportHistoryDir.getRemote();
            SonargraphLogger.logToConsoleOutput(logger, Level.SEVERE, msg, null);
            throw new IOException(msg);
        }

        Pattern extensionPattern = Pattern.compile("\\.[a-zA-Z0-9]*$");
        Matcher extensionMatcher = extensionPattern.matcher(architectReport.getRemote());
        String extension = extensionMatcher.find() ? extensionMatcher.group() : "";
        FilePath to = new FilePath(m_sonargraphReportHistoryDir, SONARGRAPH_JENKINS_REPORT_FILE_NAME_PREFIX + buildNumber + extension);
        architectReport.copyTo(to);
    }

    public void storeGeneratedReportDirectory(FilePath reportDirectory, Integer buildNumber, PrintStream logger) throws IOException,
            InterruptedException
    {
        assert reportDirectory != null : "Parameter 'reportDirectory' of method 'soterdGeneratedReportDirectory' must not be null";
        assert reportDirectory.exists() : "Parameter 'reportDirectory' must be an existing folder. '" + reportDirectory.getRemote()
                + "' does not exist.";

        if (!m_sonargraphReportHistoryDir.exists())
        {
            String msg = "Unable to create directory " + m_sonargraphReportHistoryDir.getRemote();
            SonargraphLogger.logToConsoleOutput(logger, Level.SEVERE, msg, null);
            throw new IOException(msg);
        }
        reportDirectory.copyRecursiveTo(new FilePath(m_sonargraphReportHistoryDir, "sonargraph-report-build-" + buildNumber));
    }
}
