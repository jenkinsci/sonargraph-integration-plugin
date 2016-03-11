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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import hudson.FilePath;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReportHistoryFileManagerTest
{
    private static final String reporFileName = "src/test/resources/sonargraph-sonar-report.xml";
    private static final String archReportHistoryPath = "src/test/resources/temp";
    private static final String buildReportDirectoryPath = "src/test/resources/report";
    private FilePath sonargraphReporFile;
    private static final String dummyLogFileName = "src/test/resources/dummy.log";
    private final File dummyLogFile = new File(dummyLogFileName);
    private PrintStream m_logger;

    @Before
    public void before() throws IOException, InterruptedException
    {
        removeFiles();
        if (!dummyLogFile.exists())
        {
            dummyLogFile.createNewFile();
        }
        m_logger = new PrintStream(dummyLogFileName);
    }

    @After
    public void tearDown() throws IOException, InterruptedException
    {
        if (m_logger != null)
        {
            m_logger.close();
        }
        removeFiles();
    }

    private void removeFiles() throws IOException, InterruptedException
    {
        if ((sonargraphReporFile != null) && sonargraphReporFile.exists())
        {
            sonargraphReporFile.delete();
        }
        final File historyDir = new File(archReportHistoryPath);
        if (historyDir.exists())
        {
            rm_r(historyDir);
        }
        final File buildReportDir = new File(buildReportDirectoryPath);
        if (buildReportDir.exists())
        {
            rm_r(buildReportDir);
        }
        if ((dummyLogFile != null) && dummyLogFile.exists())
        {
            dummyLogFile.delete();
        }
    }

    private static void rm_r(final File directoryToBeDeleted)
    {
        if (directoryToBeDeleted.isDirectory())
        {
            for (final File c : directoryToBeDeleted.listFiles())
            {
                rm_r(c);
            }
        }
        directoryToBeDeleted.delete();
    }

    @Test
    public void testStoreGeneratedReport() throws IOException, InterruptedException
    {
        final ReportHistoryFileManager rhfm = new ReportHistoryFileManager(new FilePath((VirtualChannel) null, archReportHistoryPath),
                "sonargraphReportHistory", m_logger);
        final Integer buildNumber = 1;
        sonargraphReporFile = new FilePath(rhfm.getReportHistoryDirectory(),
                ReportHistoryFileManager.SONARGRAPH_JENKINS_REPORT_FILE_NAME_PREFIX + buildNumber + ".xml");
        assertFalse(sonargraphReporFile.exists());

        rhfm.storeGeneratedReport(new FilePath((VirtualChannel) null, reporFileName), buildNumber, m_logger);
        assertTrue(sonargraphReporFile.exists());
    }

    @Test
    public void testStoreGeneratedReportDirectory() throws IOException, InterruptedException
    {
        final ReportHistoryFileManager rhfm = new ReportHistoryFileManager(new FilePath((VirtualChannel) null, archReportHistoryPath),
                "sonargraphReportHistory", m_logger);
        final FilePath buildReportDirectory = new FilePath((VirtualChannel) null, buildReportDirectoryPath);
        if (!buildReportDirectory.exists())
        {
            buildReportDirectory.mkdirs();
        }
        final File tesFile = new File(buildReportDirectory.getRemote(), "tesFile.xml");
        if (!tesFile.exists())
        {
            tesFile.createNewFile();
        }
        rhfm.storeGeneratedReportDirectory(buildReportDirectory, 1, m_logger);
        final String buildReportDirInHistory = "sonargraph-report-build-1";
        assertTrue(new FilePath(rhfm.getReportHistoryDirectory(), buildReportDirInHistory).exists());
        assertTrue(new File(rhfm.getReportHistoryDirectory() + "/" + buildReportDirInHistory, "tesFile.xml").exists());
    }
}
