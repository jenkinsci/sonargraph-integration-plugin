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

import hudson.model.Action;

/**
 * Extending from this class means that your action will not be visible as a
 * link in the sidebar, but since you can implement the getURLName method, It
 * will be possible to access the action by using the URL.
 * 
 * @author esteban
 *
 */
public abstract class InvisibleFromSidebarAction extends AbstractHTMLAction implements Action
{

    /**
     * Hides the Icon.
     */
    @Override
    public final String getIconFileName()
    {
        return null;
    }

    /**
     * Hides the link.
     */
    @Override
    public final String getDisplayName()
    {
        return null;
    }

}
