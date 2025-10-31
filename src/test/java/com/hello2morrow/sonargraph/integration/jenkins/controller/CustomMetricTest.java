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

import com.hello2morrow.sonargraph.integration.jenkins.persistence.MetricIdsHistory;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.TestExtension;

class CustomMetricTest extends JenkinsJobBasedTest
{
    @Test
    void testSameReportDoesNotAddMetricId() throws Exception
    {
        mavenProject.renameTo("testSameReportDoesNotAddMetricId");
        // 1st build
        MavenModuleSetBuild mmsb = jenkins.buildAndAssertSuccess(mavenProject);
        // 2nd build
        mmsb = jenkins.buildAndAssertSuccess(mavenProject);
        jenkins.assertLogContains(MetricIdsHistory.ADD_METRIC_IDS_HAD_NO_EFFECT_ON_FILE, mmsb);
    }

    @TestExtension
    public static class RunListenerImpl extends RunListener<MavenModuleSetBuild> {
        @Override
        public void onCompleted(MavenModuleSetBuild run, @NonNull TaskListener listener) {
            try {
                Arrays.stream(run.getWorkspace().list("**", "")).forEach(fp -> Logger.getLogger(CustomMetricTest.class.getName()).log(Level.INFO, fp.getRemote()));
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
