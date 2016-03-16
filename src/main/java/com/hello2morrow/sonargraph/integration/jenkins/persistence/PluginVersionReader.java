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
package com.hello2morrow.sonargraph.integration.jenkins.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;

public class PluginVersionReader
{
    public final static PluginVersionReader INSTANCE = new PluginVersionReader();
    private String m_version = "unknown";

    private PluginVersionReader()
    {
        final InputStream is = getClass().getResourceAsStream("/com/hello2morrow/sonargraph/integration/jenkins/controller/version.properties");
        final Properties props = new Properties();
        try
        {
            props.load(is);
            final Object version = props.get("version");
            if (version != null)
            {
                m_version = (String) version;
            }
        }
        catch (final IOException ex)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Failed to determine version of plugin: " + ex.getMessage(), ex);
        }
    }

    public String getVersion()
    {
        return m_version;
    }
}
