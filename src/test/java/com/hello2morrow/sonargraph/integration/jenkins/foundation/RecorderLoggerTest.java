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
package com.hello2morrow.sonargraph.integration.jenkins.foundation;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RecorderLoggerTest
{
    private static final String dummyLogFileName = "src/test/resources/dummy.log";
    private final File dummyLogFile = new File(dummyLogFileName);

    @Before
    public void before() throws IOException
    {
        removeFiles();
        if (!dummyLogFile.exists())
        {
            dummyLogFile.createNewFile();

        }
    }

    @After
    public void tearDown()
    {
        removeFiles();
    }

    private void removeFiles()
    {
        if ((dummyLogFile != null) & dummyLogFile.exists())
        {
            dummyLogFile.delete();
        }
    }

    @Test
    public void testLogToConsoleOutput() throws IOException
    {
        final PrintStream logger = new PrintStream(dummyLogFileName);
        final String testText = "test Text";
        SonargraphLogger.logToConsoleOutput(logger, Level.INFO, testText, null);
        SonargraphLogger.logToConsoleOutput(logger, Level.WARNING, testText, null);
        SonargraphLogger.logToConsoleOutput(logger, Level.SEVERE, testText, null);
        logger.close();

        final FileReader reader = new FileReader(dummyLogFile);
        final BufferedReader buffReader = new BufferedReader(reader);

        String line;
        line = buffReader.readLine();
        assertTrue(line.contains("[INFO]"));
        assertTrue(line.contains("<SONARGRAPH>"));
        assertTrue(line.contains(testText));
        line = buffReader.readLine();
        assertTrue(line.contains("[WARNING]"));
        assertTrue(line.contains("<SONARGRAPH>"));
        assertTrue(line.contains(testText));
        line = buffReader.readLine();
        assertTrue(line.contains("[SEVERE]"));
        assertTrue(line.contains("<SONARGRAPH>"));
        assertTrue(line.contains(testText));

        buffReader.close();
    }

}
