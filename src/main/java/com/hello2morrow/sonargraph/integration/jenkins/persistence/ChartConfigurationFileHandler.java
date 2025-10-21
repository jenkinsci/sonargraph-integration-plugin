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
package com.hello2morrow.sonargraph.integration.jenkins.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.logging.Level;

import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;

import hudson.model.Job;

public final class ChartConfigurationFileHandler
{
    public static final class ChartConfiguration
    {
        public boolean core = true, java, cplusplus, csharp, python;
    }

    private static final String FILENAME = "chartConfiguration.properties";
    private final File m_directory;
    private final File m_chartConfiguration;

    public ChartConfigurationFileHandler(Job<?, ?> job)
    {
        this(job.getRootDir());
    }

    public ChartConfigurationFileHandler(File directory)
    {
        m_directory = directory;
        m_chartConfiguration = new File(m_directory, FILENAME);
        if(!m_chartConfiguration.exists())
        {
            try
            {
                m_chartConfiguration.createNewFile();
            }
            catch (IOException ioe)
            {
                SonargraphLogger.INSTANCE.log(Level.SEVERE, "Failed to create chart configuration file: " + ioe.getMessage(), ioe);
            }
        }
    }

    public void store(ChartConfiguration config)
    {
        try (OutputStream os = new FileOutputStream(m_chartConfiguration))
        {
            final Properties props = new Properties();
            props.put("core", String.valueOf(config.core));
            props.put("java", String.valueOf(config.java));
            props.put("cplusplus", String.valueOf(config.cplusplus));
            props.put("csharp", String.valueOf(config.csharp));
            props.put("python", String.valueOf(config.python));
            props.store(os, "Chart Configuration");
        }
        catch (final IOException ex)
        {
            SonargraphLogger.INSTANCE.log(Level.WARNING, "Failed to store chart configuration: " + ex.getMessage(), ex);
        }
    }

    public ChartConfiguration load()
    {
        final ChartConfiguration result = new ChartConfiguration();
        try (InputStream is = new FileInputStream(m_chartConfiguration))
        {
            final Properties props = new Properties();
            props.load(is);
            result.core = Boolean.parseBoolean(props.getProperty("core", String.valueOf(result.core)));
            result.java = Boolean.parseBoolean(props.getProperty("java", String.valueOf(result.java)));
            result.cplusplus = Boolean.parseBoolean(props.getProperty("cplusplus", String.valueOf(result.cplusplus)));
            result.csharp = Boolean.parseBoolean(props.getProperty("csharp", String.valueOf(result.csharp)));
            result.python = Boolean.parseBoolean(props.getProperty("python", String.valueOf(result.python)));
        }
        catch (final IOException ex)
        {
            SonargraphLogger.INSTANCE.log(Level.WARNING, "Failed to load chart configuration: " + ex.getMessage(), ex);
        }
        return result;
    }
}
