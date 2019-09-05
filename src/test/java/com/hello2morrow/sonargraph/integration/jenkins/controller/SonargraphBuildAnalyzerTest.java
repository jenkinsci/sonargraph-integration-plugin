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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hello2morrow.sonargraph.integration.access.controller.ControllerFactory;
import com.hello2morrow.sonargraph.integration.access.controller.IMetaDataController;
import com.hello2morrow.sonargraph.integration.access.foundation.ResultWithOutcome;
import com.hello2morrow.sonargraph.integration.access.model.IExportMetaData;
import com.hello2morrow.sonargraph.integration.access.model.IMetricId.StandardName;

import hudson.FilePath;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;

public class SonargraphBuildAnalyzerTest
{
    private static final String ARCHITECTURE_VIOLATIONS = StandardName.CORE_VIOLATIONS_PARSER_DEPENDENCIES.getStandardName();
    private static final String REPORT_FILE_NAME = "./src/test/resources/AlarmClock.xml";
    private static final String METRIC_META_DATA_FILE_NAME = "./src/test/resources/sonargraphMetricMetaData.xml";
    private static final String DUMMY_LOG_FILE_NAME = "./src/test/resources/dummy.log";
    private File m_dummyLogFile;
    private PrintStream m_logger;

    @Before
    public void setUp() throws IOException
    {
        final IMetaDataController metaDataController = ControllerFactory.createMetaDataController();
        final ResultWithOutcome<IExportMetaData> result = metaDataController.loadExportMetaData(new File(METRIC_META_DATA_FILE_NAME));
        assertTrue(result.toString(), result.isSuccess());

        m_dummyLogFile = new File(DUMMY_LOG_FILE_NAME);
        if (!m_dummyLogFile.exists())
        {
            m_dummyLogFile.createNewFile();
        }
        m_logger = new PrintStream(DUMMY_LOG_FILE_NAME);
    }

    @After
    public void tearDown()
    {
        m_logger.close();
        if ((m_dummyLogFile != null) & m_dummyLogFile.exists())
        {
            m_dummyLogFile.delete();
        }
    }

    @Test
    public void testChangeBuildResultIfViolationTresholdsExceeded() throws IOException, InterruptedException
    {
        Result result = null;
        final SonargraphBuildAnalyzer analyzer = new SonargraphBuildAnalyzer(new FilePath((VirtualChannel) null, REPORT_FILE_NAME),
                m_logger);

        //Actual number of unresolved architecture violations is 2

        result = analyzer.changeBuildResultIfViolationThresholdsExceeded(8, 10);
        assertNull("No change expected if thresholds are not violated", result);

        result = analyzer.changeBuildResultIfViolationThresholdsExceeded(7, 10);
        assertEquals("Change expected if unstable threshold reached", Result.UNSTABLE, result);

        result = analyzer.changeBuildResultIfViolationThresholdsExceeded(3, 8);
        assertEquals("Change expected if unstable threshold violated", Result.UNSTABLE, result);

        result = analyzer.changeBuildResultIfViolationThresholdsExceeded(0, 7);
        assertEquals("Change expected if failure threshold reached", Result.FAILURE, result);

        result = analyzer.changeBuildResultIfViolationThresholdsExceeded(0, 6);
        assertEquals("Change expected if failure threshold violated", Result.FAILURE, result);
    }

    @Test
    public void testChangeBuildResultIfMetricValueNotZero() throws IOException, InterruptedException
    {
        final SonargraphBuildAnalyzer analyzer = new SonargraphBuildAnalyzer(new FilePath((VirtualChannel) null, REPORT_FILE_NAME),
                m_logger);
        analyzer.changeBuildResultIfMetricValueNotZero(ARCHITECTURE_VIOLATIONS, BuildActionsEnum.NOTHING.getActionCode());
        assertNull(analyzer.getOverallBuildResult());

        analyzer.changeBuildResultIfMetricValueNotZero(ARCHITECTURE_VIOLATIONS, BuildActionsEnum.UNSTABLE.getActionCode());
        assertEquals(Result.UNSTABLE, analyzer.getOverallBuildResult());

        analyzer.changeBuildResultIfMetricValueNotZero(ARCHITECTURE_VIOLATIONS, BuildActionsEnum.FAILED.getActionCode());
        assertEquals(Result.FAILURE, analyzer.getOverallBuildResult());

        analyzer.changeBuildResultIfMetricValueNotZero(ARCHITECTURE_VIOLATIONS, BuildActionsEnum.UNSTABLE.getActionCode());
        assertEquals(Result.FAILURE, analyzer.getOverallBuildResult());
    }

    @Test
    public void testChangeBuildResultIfMetricValueIsZero() throws IOException, InterruptedException
    {
        final SonargraphBuildAnalyzer analyzer = new SonargraphBuildAnalyzer(new FilePath((VirtualChannel) null, REPORT_FILE_NAME),
                m_logger);

        analyzer.changeBuildResultIfMetricValueIsZero("NumberOfViolations", BuildActionsEnum.FAILED.getActionCode());
        assertNull(analyzer.getOverallBuildResult());

        analyzer.changeBuildResultIfMetricValueIsZero("NumberOfTargetFiles", BuildActionsEnum.FAILED.getActionCode());
        assertNull(analyzer.getOverallBuildResult());
    }
}