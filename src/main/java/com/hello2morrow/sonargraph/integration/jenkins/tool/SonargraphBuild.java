/*******************************************************************************
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2016 hello2morrow GmbH
 * mailto: info AT hello2morrow DOT com
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
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *******************************************************************************/
package com.hello2morrow.sonargraph.integration.jenkins.tool;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.kohsuke.stapler.DataBoundConstructor;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;

public class SonargraphBuild extends ToolInstallation implements NodeSpecific<SonargraphBuild>, EnvironmentSpecific<SonargraphBuild>
{

    private static final long serialVersionUID = 1L;

    public SonargraphBuild(String name, String home)
    {
        super(name, home, Collections.<ToolProperty<?>> emptyList());
    }

    @DataBoundConstructor
    public SonargraphBuild(String name, String home, List<? extends ToolProperty<?>> properties)
    {
        super(name, home, properties);
    }

    @Override
    public SonargraphBuild forNode(Node node, TaskListener log) throws IOException, InterruptedException
    {
        return new SonargraphBuild(getName(), translateFor(node, log));
    }

    @Override
    public SonargraphBuild forEnvironment(EnvVars environment)
    {
        return new SonargraphBuild(getName(), environment.expand(getHome()));
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<SonargraphBuild>
    {
        public DescriptorImpl()
        {
            super();
            load();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers()
        {
            return Collections.singletonList(new SonargraphBuildInstaller(null));
        }

        @Override
        public void setInstallations(SonargraphBuild... installations)
        {
            super.setInstallations(installations);
            save();
        }

        public SonargraphBuild getSonargraphBuild(String name)
        {
            assert name != null && name.length() > 0 : "Parameter 'name' of method 'getSonargraphBuild' must not be empty";
            for (SonargraphBuild sonargraphBuild : getInstallations())
            {
                if (name.equals(sonargraphBuild.getName()))
                {
                    return sonargraphBuild;
                }
            }
            SonargraphLogger.INSTANCE.log(Level.WARNING, "Unknown Sonargraph Build: " + name);
            for (SonargraphBuild sonargraphBuild : getInstallations())
            {
                SonargraphLogger.INSTANCE.log(Level.WARNING, "Found Sonargraph Build: " + sonargraphBuild.getName());
            }
            return null;
        }

        @Override
        protected FormValidation checkHomeDirectory(File home)
        {
            // TODO Auto-generated method stub
            return super.checkHomeDirectory(home);
        }

        @Override
        public String getDisplayName()
        {
            return "Sonargraph Build";
        }

    }

}
