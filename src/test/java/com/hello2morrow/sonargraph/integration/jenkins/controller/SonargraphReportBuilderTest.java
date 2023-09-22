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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import hudson.util.VersionNumber;

public class SonargraphReportBuilderTest
{
    @Test
    public void testGetLanguages()
    {
        assertEquals("No language means all languages", "Java,CPlusPlus,CSharp,Python",
                SonargraphReportBuilder.getLanguages(false, false, false, false));
        assertEquals("All languages", "Java,CPlusPlus,CSharp,Python", SonargraphReportBuilder.getLanguages(true, true, true, true));
        assertEquals("Java and CSharp", "Java,CSharp", SonargraphReportBuilder.getLanguages(true, false, true, false));
        assertEquals("Java and CPlusPlus", "Java,CPlusPlus", SonargraphReportBuilder.getLanguages(true, true, false, false));
        assertEquals("Java and Python", "Java,Python", SonargraphReportBuilder.getLanguages(true, false, false, true));
        assertEquals("CPlusPlus and CSharp", "CPlusPlus,CSharp", SonargraphReportBuilder.getLanguages(false, true, true, false));
        assertEquals("CPlusPlus and Python", "CPlusPlus,Python", SonargraphReportBuilder.getLanguages(false, true, false, true));
        assertEquals("CSharp and Python", "CSharp,Python", SonargraphReportBuilder.getLanguages(false, false, true, true));
        assertEquals("Java only", "Java", SonargraphReportBuilder.getLanguages(true, false, false, false));
        assertEquals("CSharp only", "CSharp", SonargraphReportBuilder.getLanguages(false, false, true, false));
        assertEquals("CPlusPlus only", "CPlusPlus", SonargraphReportBuilder.getLanguages(false, true, false, false));
        assertEquals("Python only", "Python", SonargraphReportBuilder.getLanguages(false, false, false, true));
    }

    @Test
    public void testUrlEncoding()
    {
        String url = "http://www.hello2morrow.com";
        try
        {
            String encoded = URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
            System.out.println(encoded);
        }
        catch (UnsupportedEncodingException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testGetVersionFromJar()
    {
        final String jarName = "com.hello2morrow.sonargraph.build_9.12.0.100_2019-08-29-18-01.jar";
        final VersionNumber version = SonargraphReportBuilder.getVersionFromJarName(jarName);
        assertEquals(9, version.getDigitAt(0));
        assertEquals(12, version.getDigitAt(1));
        assertEquals(0, version.getDigitAt(2));
        assertEquals(100, version.getDigitAt(3));
    }

    @Test
    public void testVersionNumberIsNewerThan()
    {
        final VersionNumber number1 = new VersionNumber("9.12");
        final VersionNumber number2 = new VersionNumber("9.12.0");
        final VersionNumber number3 = new VersionNumber("9.12.0.100");
        final VersionNumber number4 = new VersionNumber("9.12.1");

        assertFalse(number1.isNewerThan(number1));
        assertFalse(number2.isNewerThan(number2));
        assertFalse(number3.isNewerThan(number3));
        assertFalse(number4.isNewerThan(number4));

        assertTrue(number4.isNewerThan(number3));
        assertTrue(number4.isNewerThan(number2));
        assertTrue(number4.isNewerThan(number1));
        assertTrue(number3.isNewerThan(number2));
        assertTrue(number3.isNewerThan(number1));

        //VersionNumbers are 'nirmalized', this means trailing '0' is removed
        assertEquals(number1, number2);
        assertFalse(number2.isNewerThan(number1));
    }
}