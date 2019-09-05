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

import java.io.IOException;
import java.util.Collections;

import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuild;
import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuildInstaller;

import hudson.tools.InstallSourceProperty;
import jenkins.model.Jenkins;

public final class SonargraphBuildInstallations
{
    private SonargraphBuildInstallations()
    {
        super();
    }

    public static SonargraphBuild configureSonargraphBuildNewest()
    {
        try
        {
            final SonargraphBuildInstaller installer = new SonargraphBuildInstaller("newest");
            final SonargraphBuild sonargraphBuild = new SonargraphBuild("newest", getInstallationDirectory("newest"),
                    Collections.singletonList(new InstallSourceProperty(Collections.singletonList(installer))));
            Jenkins.getInstance().getDescriptorByType(SonargraphBuild.DescriptorImpl.class).setInstallations(sonargraphBuild);
            return sonargraphBuild;
        }
        catch (final IOException e)
        {
        }
        return null;
    }

    private static String getInstallationDirectory(final String subdirectory)
    {
        final String directory = System.getProperty("toolDirectory");
        if (directory != null)
        {
            return directory + "/Tools/SonargraphBuild/" + subdirectory;
        }
        return "";
    }
}