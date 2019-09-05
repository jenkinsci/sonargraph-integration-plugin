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

import org.junit.Test;

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
}
