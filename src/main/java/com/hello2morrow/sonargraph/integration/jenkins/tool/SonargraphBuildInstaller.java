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
package com.hello2morrow.sonargraph.integration.jenkins.tool;

import java.io.IOException;

import edu.umd.cs.findbugs.annotations.NonNull;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstallerDescriptor;

public final class SonargraphBuildInstaller extends DownloadFromUrlInstaller
{
    private final Installable installable;

    @DataBoundConstructor
    public SonargraphBuildInstaller(@NonNull final Installable installable)
    {
        super(installable.id);
        this.installable = installable;
    }

    @Override
    public Installable getInstallable()
    {
        return this.installable;
    }

    @Extension
    public static final class DescriptorImpl extends ToolInstallerDescriptor<SonargraphBuildInstaller>
    {
        public DescriptorImpl()
        {
        }

        @Override
        public boolean isApplicable(final Class<? extends ToolInstallation> toolType)
        {
            return toolType == SonargraphBuild.class;
        }

        @Override
        public @NonNull String getDisplayName()
        {
            return "Install from hello2morrow";
        }
    }
}