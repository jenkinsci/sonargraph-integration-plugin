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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.hello2morrow.sonargraph.integration.access.controller.ControllerFactory;
import com.hello2morrow.sonargraph.integration.access.controller.IMetaDataController;
import com.hello2morrow.sonargraph.integration.access.foundation.ResultWithOutcome;
import com.hello2morrow.sonargraph.integration.access.model.IExportMetaData;
import com.hello2morrow.sonargraph.integration.access.model.IMetricId.StandardName;

import hudson.FilePath;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;

class SonargraphBuildAnalyzerTest
{
    private static final String ARCHITECTURE_VIOLATIONS = StandardName.CORE_VIOLATIONS_PARSER_DEPENDENCIES.getStandardName();
    private static final String REPORT_FILE_NAME = "./src/test/resources/AlarmClock.xml";
    private static final String METRIC_META_DATA_FILE_NAME = "./src/test/resources/sonargraphMetricMetaData.xml";
    private static final String DUMMY_LOG_FILE_NAME = "./src/test/resources/dummy.log";
    private File m_dummyLogFile;
    private PrintStream m_logger;

    @BeforeEach
    void setUp() throws Exception
    {
        final IMetaDataController metaDataController = ControllerFactory.createMetaDataController();
        final ResultWithOutcome<IExportMetaData> result = metaDataController.loadExportMetaData(new File(METRIC_META_DATA_FILE_NAME));
        assertTrue(result.isSuccess(), result.toString());

        m_dummyLogFile = new File(DUMMY_LOG_FILE_NAME);
        if (!m_dummyLogFile.exists())
        {
            m_dummyLogFile.createNewFile();
        }
        m_logger = new PrintStream(DUMMY_LOG_FILE_NAME);
    }

    @AfterEach
    void tearDown()
    {
        m_logger.close();
        if ((m_dummyLogFile != null) & m_dummyLogFile.exists())
        {
            m_dummyLogFile.delete();
        }
    }

    @Test
    void testChangeBuildResultIfViolationThresholdsExceeded() throws Exception
    {
        Result result = null;
        final SonargraphBuildAnalyzer analyzer = new SonargraphBuildAnalyzer(new FilePath((VirtualChannel) null, REPORT_FILE_NAME),
                m_logger);

        //Actual number of unresolved architecture violations is 2

        result = analyzer.changeBuildResultIfViolationThresholdsExceeded(8, 10);
        assertNull(result, "No change expected if thresholds are not violated");

        result = analyzer.changeBuildResultIfViolationThresholdsExceeded(7, 10);
        assertEquals(Result.UNSTABLE, result, "Change expected if unstable threshold reached");

        result = analyzer.changeBuildResultIfViolationThresholdsExceeded(3, 8);
        assertEquals(Result.UNSTABLE, result, "Change expected if unstable threshold violated");

        result = analyzer.changeBuildResultIfViolationThresholdsExceeded(0, 7);
        assertEquals(Result.FAILURE, result, "Change expected if failure threshold reached");

        result = analyzer.changeBuildResultIfViolationThresholdsExceeded(0, 6);
        assertEquals(Result.FAILURE, result, "Change expected if failure threshold violated");
    }

    @Test
    void testChangeBuildResultIfMetricValueNotZero() throws Exception
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
    void testChangeBuildResultIfMetricValueIsZero() throws Exception
    {
        final SonargraphBuildAnalyzer analyzer = new SonargraphBuildAnalyzer(new FilePath((VirtualChannel) null, REPORT_FILE_NAME),
                m_logger);

        analyzer.changeBuildResultIfMetricValueIsZero("NumberOfViolations", BuildActionsEnum.FAILED.getActionCode());
        assertNull(analyzer.getOverallBuildResult());

        analyzer.changeBuildResultIfMetricValueIsZero("NumberOfTargetFiles", BuildActionsEnum.FAILED.getActionCode());
        assertNull(analyzer.getOverallBuildResult());
    }
}