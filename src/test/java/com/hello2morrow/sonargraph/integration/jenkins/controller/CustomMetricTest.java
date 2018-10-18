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

import org.junit.Test;

import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricIdsHistory;

import hudson.maven.MavenModuleSetBuild;

public class CustomMetricTest extends JenkinsJobBasedTest
{

    @Test
    public void testSameReportDoesNotAddMetricId() throws Exception
    {
        mavenProject.renameTo("testSameReportDoesNotAddMetricId");
        // 1st build
        MavenModuleSetBuild mmsb = jenkins.buildAndAssertSuccess(mavenProject);
        // 2nd build
        mmsb = jenkins.buildAndAssertSuccess(mavenProject);
        jenkins.assertLogContains(MetricIdsHistory.ADD_METRIC_IDS_HAD_NO_EFFECT_ON_FILE, mmsb);
    }
}
