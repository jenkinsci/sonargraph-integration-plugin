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

enum BuildActionsEnum
{
    /** Build not affected. */
    NOTHING("Don't mark", "nothing"),

    /** Make the build unstable. */
    UNSTABLE("Build unstable", "unstable"),

    /** Make the build failed. */
    FAILED("Build failed", "failed");

    /** Action name. Used for the combobox in the UI. */
    private String m_actionName;

    /** Action code. Used for the logic of the build. */
    private String m_actionCode;

    private BuildActionsEnum(final String actionName, final String actionCode)
    {
        m_actionName = actionName;
        m_actionCode = actionCode;
    }

    public String getActionName()
    {
        return m_actionName;
    }

    public String getActionCode()
    {
        return m_actionCode;
    }
}
