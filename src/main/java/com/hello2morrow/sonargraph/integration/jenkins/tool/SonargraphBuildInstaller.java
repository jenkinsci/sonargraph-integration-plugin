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
package com.hello2morrow.sonargraph.integration.jenkins.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.kohsuke.stapler.DataBoundConstructor;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;

import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstallerDescriptor;
import net.sf.json.JSONObject;

public final class SonargraphBuildInstaller extends DownloadFromUrlInstaller
{
    @DataBoundConstructor
    public SonargraphBuildInstaller(final String id)
    {
        super(id);
    }

    @Override
    public Installable getInstallable() throws IOException
    {
        for (final Installable i : ((DescriptorImpl) getDescriptor()).getInstallables())
        {
            if (id.equals(i.id))
            {
                return i;
            }
        }
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends ToolInstallerDescriptor<SonargraphBuildInstaller>
    {
        private static final long THIRTY_MINUTES = 30 * 60 * 1000L;
        private static final String SONARGRAPH_BUILD_JSON = "http://eclipse.hello2morrow.com/jenkins/sonargraphBuild/sonargraphBuild.json";
        List<Installable> installables = null;
        long lastMillis = 0;

        public DescriptorImpl()
        {
        }

        @Override
        public boolean isApplicable(final Class<? extends ToolInstallation> toolType)
        {
            return toolType == SonargraphBuild.class;
        }

        @Override
        public String getDisplayName()
        {
            return "Install from hello2morrow";
        }

        public List<Installable> getInstallables()
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
                    installables = Arrays.asList(((InstallableList) JSONObject.toBean(d, InstallableList.class)).list);
                    SonargraphLogger.INSTANCE.log(Level.INFO, "Got list of {0} installables.", installables.size());

                }
                catch (final Exception e)
                {
                    SonargraphLogger.INSTANCE.log(Level.SEVERE, "{0} while getting installables: {1}",
                            new Object[] { e.getClass().getName(), e.getMessage() });
                    installables = Collections.emptyList();
                }
                finally
                {
                    lastMillis = System.currentTimeMillis();
                }

            }
            return installables;
        }

        private boolean checkAgain()
        {
            final boolean result = System.currentTimeMillis() - lastMillis > THIRTY_MINUTES;
            return result;
        }
    }
}