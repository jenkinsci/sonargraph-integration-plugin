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

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;

public abstract class AbstractBuildStepDescriptor extends BuildStepDescriptor<Publisher>
{
    public AbstractBuildStepDescriptor()
    {
        super();
        load();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType)
    {
        return true;
    }

    public ListBoxModel doFillArchitectureViolationsActionItems()
    {
        return createListWithActions();
    }

    public ListBoxModel doFillUnassignedTypesActionItems()
    {
        return createListWithActions();
    }

    public ListBoxModel doFillCyclicElementsActionItems()
    {
        return createListWithActions();
    }

    public ListBoxModel doFillThresholdViolationsActionItems()
    {
        return createListWithActions();
    }

    public ListBoxModel doFillArchitectureWarningsActionItems()
    {
        return createListWithActions();
    }

    public ListBoxModel doFillWorkspaceWarningsActionItems()
    {
        return createListWithActions();
    }

    public ListBoxModel doFillWorkItemsActionItems()
    {
        return createListWithActions();
    }

    public ListBoxModel doFillEmptyWorkspaceActionItems()
    {
        return createListWithActions();
    }

    private ListBoxModel createListWithActions()
    {
        final ListBoxModel items = new ListBoxModel();
        for (final BuildActionsEnum action : BuildActionsEnum.values())
        {
            items.add(action.getActionName(), action.getActionCode());
        }
        return items;
    }

}
