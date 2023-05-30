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
package com.hello2morrow.sonargraph.integration.jenkins.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hello2morrow.sonargraph.integration.jenkins.controller.ConfigParameters;

import hudson.FilePath;
import hudson.remoting.VirtualChannel;

public class ReportHistoryFileManagerTest
{
    private static final String archReportHistoryPath = "src/test/resources/temp";
    private static final String buildReportDirectoryPath = "src/test/resources/report";
    private static final String dummyLogFileName = "src/test/resources/dummy.log";
    private final File dummyLogFile = new File(dummyLogFileName);
    private PrintStream m_logger;

    @Before
    public void before() throws IOException
    {
        removeFiles();
        if (!dummyLogFile.exists())
        {
            dummyLogFile.createNewFile();
        }
        m_logger = new PrintStream(dummyLogFileName);
    }

    @After
    public void tearDown()
    {
        if (m_logger != null)
        {
            m_logger.close();
        }
        removeFiles();
    }

    private void removeFiles()
    {
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
    public void testStoreGeneratedReportDirectory() throws IOException, InterruptedException
    {
        final ReportHistoryFileManager rhfm = new ReportHistoryFileManager(new FilePath((VirtualChannel) null, archReportHistoryPath),
                "sonargraphReportHistory", ConfigParameters.SONARGRAPH_REPORT_FILE_NAME.getValue(), m_logger);
        final FilePath buildReportDirectory = new FilePath((VirtualChannel) null, buildReportDirectoryPath);
        if (!buildReportDirectory.exists())
        {
            buildReportDirectory.mkdirs();
        }
        final File testFile = new File(buildReportDirectory.getRemote(), "testFile.xml");
        if (!testFile.exists())
        {
            testFile.createNewFile();
        }
        buildAndCheckLatest(rhfm, buildReportDirectory, 1);
    }
    
    @Test
    public void testLatestAfterStoreGeneratedReportDirectory() throws IOException, InterruptedException
    {
        final ReportHistoryFileManager rhfm = new ReportHistoryFileManager(new FilePath((VirtualChannel) null, archReportHistoryPath),
                "sonargraphReportHistory", ConfigParameters.SONARGRAPH_REPORT_FILE_NAME.getValue(), m_logger);
        final FilePath buildReportDirectory = new FilePath((VirtualChannel) null, buildReportDirectoryPath);
        if (!buildReportDirectory.exists())
        {
            buildReportDirectory.mkdirs();
        }
        final File testFile = new File(buildReportDirectory.getRemote(), "testFile.xml");
        if (!testFile.exists())
        {
            testFile.createNewFile();
        }
        buildAndCheckLatest(rhfm, buildReportDirectory, 1);
        buildAndCheckLatest(rhfm, buildReportDirectory, 2);
        buildAndCheckLatest(rhfm, buildReportDirectory, 3);
        
        // test for changing 'next build number' to a larger value, and then back to a lower value
        buildAndCheckLatest(rhfm, buildReportDirectory, 999);
        buildAndCheckLatest(rhfm, buildReportDirectory, 55);
        buildAndCheckLatest(rhfm, buildReportDirectory, 2);
        
    }

    private void buildAndCheckLatest(final ReportHistoryFileManager rhfm, final FilePath buildReportDirectory, final int build) throws IOException, InterruptedException
    {
        rhfm.storeGeneratedReportDirectory(buildReportDirectory, "testFile", build, m_logger);
        final String buildReportDirInHistory = "sonargraph-report-build-" + build;
        assertTrue(new FilePath(rhfm.getReportHistoryDirectory(), buildReportDirInHistory).exists());
        assertTrue(new File(rhfm.getReportHistoryDirectory() + "/" + buildReportDirInHistory,
                ConfigParameters.SONARGRAPH_REPORT_FILE_NAME.getValue() + ".xml").exists());
        
        // check "latest" symlink
        final FilePath latestSymlink = new FilePath(rhfm.getReportHistoryDirectory(), "latest");
        assertTrue(latestSymlink.exists());
        final String linkTarget = latestSymlink.readLink();
        assertNotNull(linkTarget);
        assertEquals(buildReportDirInHistory , linkTarget);
    }
}
