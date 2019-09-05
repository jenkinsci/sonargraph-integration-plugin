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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import hudson.maven.MavenModuleSetBuild;
import hudson.model.JDK;

public class DefaultJDKTest extends JenkinsJobBasedTest
{

    @Test
    public void testUsingDefaultJDK() throws Exception
    {
        mavenProject.renameTo("testUsingDefaultJDK");
        final MavenModuleSetBuild mmsb = jenkins.buildAndAssertSuccess(mavenProject);
        jenkins.assertLogContains("Using default JDK 'default' for Sonargraph Build.", mmsb);
    }

    @Test
    public void testNoJDK() throws Exception
    {
        jenkins.getInstance().setJDKs(new ArrayList<JDK>());

        mavenProject.renameTo("testNoJDK");
        final MavenModuleSetBuild mmsb = jenkins.buildAndAssertSuccess(mavenProject);
        jenkins.assertLogContains("Must try to use JDK Jenkins is running with for Sonargraph Build.", mmsb);
    }

    @Test
    public void testMultipleJDK() throws Exception
    {
        final List<JDK> jdks = new ArrayList<>();
        jdks.addAll(jenkins.getInstance().getJDKs());
        jdks.add(new JDK("dummy1", ""));
        jdks.add(new JDK("dummy2", ""));
        jenkins.getInstance().setJDKs(jdks);

        mavenProject.renameTo("testMultipleJDK");
        final MavenModuleSetBuild mmsb = jenkins.buildAndAssertSuccess(mavenProject);
        jenkins.assertLogContains("There are multiple JDKs, please configure one of them.", mmsb);
    }

}
