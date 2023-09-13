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
package com.hello2morrow.sonargraph.integration.jenkins.foundation;

import hudson.util.VersionNumber;

public final class SonargraphUtil
{
    private SonargraphUtil()
    {
        super();
    }

    public static VersionNumber getVersionFromJarName(String jarName)
    {
        int first = jarName.indexOf("_");
        int second = jarName.indexOf("_", first + 1);
        if (first != -1 && second != -1)
        {
            final String version = jarName.substring(first + 1, second);
            return new VersionNumber(version);
        }
        return new VersionNumber("0.0");
    }
}