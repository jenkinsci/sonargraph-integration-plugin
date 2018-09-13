/*
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2018 hello2morrow GmbH
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.ToolInstallations;

import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuild;
import com.hello2morrow.sonargraph.integration.jenkins.tool.SonargraphBuildInstaller;

import hudson.maven.MavenModuleSet;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.JDK;
import hudson.tasks.Publisher;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;

public class DefaultJDKTest
{

    @Rule
    public JenkinsRule j = new JenkinsRule();

    MavenModuleSet mavenProject;

    @Before
    public void before() throws Exception
    {
        ToolInstallations.configureMaven35();
        j.getInstance().getDescriptorList(ToolInstaller.class).add(new SonargraphBuildInstaller.DescriptorImpl());
        j.getInstance().getDescriptorList(ToolInstallation.class).add(new SonargraphBuild.DescriptorImpl());
        j.getInstance().getDescriptorList(Publisher.class).add(new SonargraphReportBuilder.DescriptorImpl());
        SonargraphBuildInstallations.configureSonargraphBuildNewest();

        mavenProject = j.createProject(MavenModuleSet.class);
        mavenProject.setGoals("compile");
        Publisher sonargraph = createSonargraphReportBuilder();
        mavenProject.getPublishers().add(sonargraph);

        // Extracts "src/test/resources/alarm-clock.zip" into the workspace
        URL resource = getClass().getResource("/alarm-clock.zip");
        mavenProject.setScm(new ExtractResourceSCM(resource));
    }

    @Test
    public void testUsingDefaultJDK() throws Exception
    {
        mavenProject.renameTo("testUsingDefaultJDK");
        MavenModuleSetBuild mmsb = j.buildAndAssertSuccess(mavenProject);
        j.assertLogContains("Using default JDK 'default' for Sonargraph Build.", mmsb);
    }

    @Test
    public void testNoJDK() throws Exception
    {
        j.getInstance().setJDKs(new ArrayList<JDK>());

        mavenProject.renameTo("testNoJDK");
        MavenModuleSetBuild mmsb = j.buildAndAssertSuccess(mavenProject);
        j.assertLogContains("Must try to use JDK Jenkins is running with for Sonargraph Build.", mmsb);
    }

    @Test
    public void testMultipleJDK() throws Exception
    {
        List<JDK> jdks = new ArrayList<>();
        jdks.addAll(j.getInstance().getJDKs());
        jdks.add(new JDK("dummy1", ""));
        jdks.add(new JDK("dummy2", ""));
        j.getInstance().setJDKs(jdks);

        mavenProject.renameTo("testMultipleJDK");
        MavenModuleSetBuild mmsb = j.buildAndAssertSuccess(mavenProject);
        j.assertLogContains("There are multiple JDKs, please configure one of them.", mmsb);
    }

    private static SonargraphReportBuilder createSonargraphReportBuilder()
    {
        List<Metric> metrics = Collections.emptyList();
        String metaDataFile = "";
        String systemDirectory = "AlarmClock.sonargraph";
        String qualityModelFile = "";
        String virtualModel = "Modifiable.vm";
        String reportPath = "";
        String reportGeneration = "";
        String chartConfiguration = "";
        String architectureViolationsAction = "";
        String unassignedTypesAction = "";
        String cyclicElementsAction = "";
        String thresholdViolationsAction = "";
        String architectureWarningsAction = "";
        String workspaceWarningsAction = "";
        String workItemsAction = "";
        String emptyWorkspaceAction = "";
        boolean languageJava = false;
        boolean languageCSharp = false;
        boolean languageCPlusPlus = false;
        String sonargraphBuildJDK = "";
        String sonargraphBuildVersion = "newest";
        String activationCode = System.getProperty("sonargraph.activationcode.test", "");
        String licenseFile = "";
        String workspaceProfile = "";
        String snapshotDirectory = "";
        String snapshotFileName = "";
        String logLevel = "";
        String logFile = "";
        String elementCountToSplitHtmlReport = "1000";
        String maxElementCountForHtmlDetailsPage = "2000";
        boolean splitByModule = false;

        return new SonargraphReportBuilder(metrics, metaDataFile, systemDirectory, qualityModelFile, virtualModel, reportPath, reportGeneration,
                chartConfiguration, architectureViolationsAction, unassignedTypesAction, cyclicElementsAction, thresholdViolationsAction,
                architectureWarningsAction, workspaceWarningsAction, workItemsAction, emptyWorkspaceAction, languageJava, languageCSharp,
                languageCPlusPlus, sonargraphBuildJDK, sonargraphBuildVersion, activationCode, licenseFile, workspaceProfile, snapshotDirectory,
                snapshotFileName, logLevel, logFile, elementCountToSplitHtmlReport, maxElementCountForHtmlDetailsPage, splitByModule);
    }
}
