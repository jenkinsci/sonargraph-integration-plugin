/*
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2020 hello2morrow GmbH
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

import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuild;
import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuildInstaller;

import hudson.maven.MavenModuleSet;
import hudson.tasks.Publisher;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;

public abstract class JenkinsJobBasedTest
{
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    public MavenModuleSet mavenProject;

    @Before
    public void before() throws Exception
    {
        ToolInstallations.configureMaven35();
        jenkins.getInstance().getDescriptorList(ToolInstaller.class).add(new SonargraphBuildInstaller.DescriptorImpl());
        jenkins.getInstance().getDescriptorList(ToolInstallation.class).add(new SonargraphBuild.DescriptorImpl());
        jenkins.getInstance().getDescriptorList(Publisher.class).add(new SonargraphReportBuilder.DescriptorImpl());
        SonargraphBuildInstallations.configureSonargraphBuildNewest();

        mavenProject = jenkins.createProject(MavenModuleSet.class);
        mavenProject.setGoals("compile");
        final Publisher sonargraph = createSonargraphReportBuilder();
        mavenProject.getPublishers().add(sonargraph);

        // Extracts "src/test/resources/alarm-clock.zip" into the workspace
        final URL resource = getClass().getResource("/alarm-clock.zip");
        mavenProject.setScm(new ExtractResourceSCM(resource));
    }
    
    private static SonargraphReportBuilder createSonargraphReportBuilder()
    {
        final String systemDirectory = "AlarmClock.sonargraph";
        final String virtualModel = "Modifiable.vm";
        final String sonargraphBuildVersion = "newest";
        final String activationCode = System.getProperty("sonargraph.activationcode.test", "");

        final SonargraphReportBuilder result = new SonargraphReportBuilder();
        result.setSystemDirectory(systemDirectory);
        result.setActivationCode(activationCode);
        result.setVirtualModel(virtualModel);
        result.setSonargraphBuildVersion(sonargraphBuildVersion);
        
        return result;
    }
}
