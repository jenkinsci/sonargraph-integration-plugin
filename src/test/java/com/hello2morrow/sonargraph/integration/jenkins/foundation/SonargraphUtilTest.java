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
package com.hello2morrow.sonargraph.integration.jenkins.foundation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.util.VersionNumber;
import org.junit.jupiter.api.Test;

class SonargraphUtilTest
{

    @Test
    void testGetVersionFromJar()
    {
        final String jarName = "com.hello2morrow.sonargraph.build_9.12.0.100_2019-08-29-18-01.jar";
        final VersionNumber version = SonargraphUtil.getVersionFromJarName(jarName);
        assertEquals(9, version.getDigitAt(0));
        assertEquals(12, version.getDigitAt(1));
        assertEquals(0, version.getDigitAt(2));
        assertEquals(100, version.getDigitAt(3));
    }

    @Test
    void testVersionNumberIsNewerThan()
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
