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
package com.hello2morrow.sonargraph.integration.jenkins.controller;

import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.Project;
import hudson.model.TopLevelItem;
import hudson.model.Queue.FlyweightTask;

public final class JobCategory
{
    private static final String ORG_JENKINSCI_PLUGINS_WORKFLOW_JOB_WORKFLOWJOB = "org.jenkinsci.plugins.workflow.job.WorkflowJob";

    private JobCategory()
    {
        // no instances
    }

    /**
     * Checks if a job is a pipeline/workflow job.
     * 
     * @param job the job.
     * @return true if job is a pipeline/workflow job.
     */
    public static boolean isPipelineJob(final Job<?, ?> job)
    {
        // We cannot assume an installed 'workflow-job' plugin with that class, therefore check the classname.
        // Luckily class WorkflowJob is final, and a direct subclass of Job.
        final boolean workflowJob = job.getClass().getName().equals(ORG_JENKINSCI_PLUGINS_WORKFLOW_JOB_WORKFLOWJOB);
        return workflowJob;
    }

    /**
     * Checks if a project is 'relevant' for Sonargraph.
     * 
     * @param project the project.
     * @return true if project is relevant for Sonargraph.
     */
    public static boolean isRelevantProject(final AbstractProject<?, ?> project)
    {
        final boolean relevant = project instanceof Project || (project instanceof TopLevelItem && !(project instanceof FlyweightTask));
        return relevant;
    }
}
