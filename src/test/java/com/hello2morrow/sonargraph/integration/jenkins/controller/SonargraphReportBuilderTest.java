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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class SonargraphReportBuilderTest
{
    @Test
    void testGetLanguages()
    {
        assertEquals("Java,CPlusPlus,CSharp,Python",
                SonargraphReportBuilder.getLanguages(false, false, false, false),
                "No language means all languages");
        assertEquals("Java,CPlusPlus,CSharp,Python", SonargraphReportBuilder.getLanguages(true, true, true, true), "All languages");
        assertEquals("Java,CSharp", SonargraphReportBuilder.getLanguages(true, false, true, false), "Java and CSharp");
        assertEquals("Java,CPlusPlus", SonargraphReportBuilder.getLanguages(true, true, false, false), "Java and CPlusPlus");
        assertEquals("Java,Python", SonargraphReportBuilder.getLanguages(true, false, false, true), "Java and Python");
        assertEquals("CPlusPlus,CSharp", SonargraphReportBuilder.getLanguages(false, true, true, false), "CPlusPlus and CSharp");
        assertEquals("CPlusPlus,Python", SonargraphReportBuilder.getLanguages(false, true, false, true), "CPlusPlus and Python");
        assertEquals("CSharp,Python", SonargraphReportBuilder.getLanguages(false, false, true, true), "CSharp and Python");
        assertEquals("Java", SonargraphReportBuilder.getLanguages(true, false, false, false), "Java only");
        assertEquals("CSharp", SonargraphReportBuilder.getLanguages(false, false, true, false), "CSharp only");
        assertEquals("CPlusPlus", SonargraphReportBuilder.getLanguages(false, true, false, false), "CPlusPlus only");
        assertEquals("Python", SonargraphReportBuilder.getLanguages(false, false, false, true), "Python only");
    }

    @Test
    void testUrlEncoding()
    {
        String url = "http://www.hello2morrow.com";
        String encoded = URLEncoder.encode(url, StandardCharsets.UTF_8);
        System.out.println(encoded);
    }
}