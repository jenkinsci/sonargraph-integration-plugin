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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuild;
import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuildInstaller;

import hudson.ProxyConfiguration;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.InstallSourceProperty;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

public final class SonargraphBuildInstallations
{
    private static final long THIRTY_MINUTES = 30 * 60 * 1000L;
    private static final String SONARGRAPH_BUILD_JSON = "https://eclipse.hello2morrow.com/jenkins/sonargraphBuild/sonargraphBuild.json";
    private static SonargraphBuildInstallations INSTANCE = new SonargraphBuildInstallations();

    private long lastMillis = 0;
    private List<DownloadFromUrlInstaller.Installable> installables = null;

    private SonargraphBuildInstallations()
    {
        super();
    }

    private void retrieveInstallables()
    {
        if (installables == null || checkAgain())
        {
            SonargraphLogger.INSTANCE.log(Level.INFO, "Trying to get list of installables from {0} ...", SONARGRAPH_BUILD_JSON);
            try (InputStream in = ProxyConfiguration.getInputStream(new URL(SONARGRAPH_BUILD_JSON));
                 BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, "UTF-8")))
            {
                final StringBuilder responseStrBuilder = new StringBuilder();
                String inputStr;

                while ((inputStr = streamReader.readLine()) != null)
                {
                    responseStrBuilder.append(inputStr);
                }
                final JSONObject d = JSONObject.fromObject(responseStrBuilder.toString());
                installables = Arrays.asList(((DownloadFromUrlInstaller.InstallableList) JSONObject.toBean(d, DownloadFromUrlInstaller.InstallableList.class)).list);
                SonargraphLogger.INSTANCE.log(Level.INFO, "Got list of {0} installables.", installables.size());
            }
            catch (final Exception e)
            {
                SonargraphLogger.INSTANCE.log(Level.SEVERE, "{0} while getting installables: {1}",
                        new Object[]{e.getClass().getName(), e.getMessage()});
                installables = Collections.emptyList();
            }
            finally
            {
                lastMillis = System.currentTimeMillis();
            }
        }
    }

    private boolean checkAgain()
    {
        return System.currentTimeMillis() - lastMillis > THIRTY_MINUTES;
    }

    private DownloadFromUrlInstaller.Installable getNewest()
    {
        retrieveInstallables();
        if (installables.size() >= 2)
        {
            // The first one is "newest", the second one is the version of the newest SonargraphBuild
            return installables.get(1);
        }
        return null;
    }

    public static DownloadFromUrlInstaller.Installable getSonargraphBuildNewest()
    {
        try
        {
            var installable = INSTANCE.getNewest();

            if (installable != null)
            {
                final SonargraphBuildInstaller installer = new SonargraphBuildInstaller(installable);
                final SonargraphBuild sonargraphBuild = new SonargraphBuild(installable.id, getInstallationDirectory(installable.id),
                        Collections.singletonList(new InstallSourceProperty(Collections.singletonList(installer))));
                Jenkins.get().getDescriptorByType(SonargraphBuild.DescriptorImpl.class).setInstallations(sonargraphBuild);
                return installable;
            }
        }
        catch (final IOException e)
        {
            // No action required.
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