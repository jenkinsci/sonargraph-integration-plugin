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
package com.hello2morrow.sonargraph.integration.jenkins.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

import com.hello2morrow.sonargraph.integration.access.foundation.ResultCause;
import com.hello2morrow.sonargraph.integration.access.foundation.ResultWithOutcome;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.model.IMetricIdsHistoryProvider;
import com.hello2morrow.sonargraph.integration.jenkins.model.MetricIds;

public class MetricIdsHistory implements IMetricIdsHistoryProvider
{
    public static final String ADD_METRIC_IDS_HAD_NO_EFFECT_ON_FILE = "Add metricIds had no effect on file ";
    public static final String OVERRIDE_METRIC_IDS_FILE = "Override metricIds file ";

    private final File file;
    private long lastModified = 0;
    private MetricIds cachedMetricIds = null;

    public MetricIdsHistory(final File historyFile)
    {
        this.file = historyFile;

        if (!this.file.exists())
        {
            try
            {
                SonargraphLogger.INSTANCE.log(Level.FINE, "Create new empty MetricIds JSON file {0}", this.file.getAbsolutePath());
                this.file.createNewFile();
                storeMetricIds(new MetricIds());
            }
            catch (final IOException ex)
            {
                SonargraphLogger.INSTANCE.log(Level.SEVERE, "Failed to create new MetricIds JSON file " + this.file.getAbsolutePath(), ex);
            }
        }
    }

    @Override
    public ResultWithOutcome<MetricIds> readMetricIds()
    {
        final ResultWithOutcome<MetricIds> result = new ResultWithOutcome<>("Read metricIds");
        String jsonString;
        long modified = this.file.lastModified();
        if (modified != this.lastModified || this.cachedMetricIds == null)
        {
            this.lastModified = modified;
            try
            {
                SonargraphLogger.INSTANCE.log(Level.FINE, "Read metricIds file {0}", getStorageName());
                jsonString = TextFileReader.readLargeTextFile(this.file);
                this.cachedMetricIds = MetricIds.fromJSON(jsonString);
                result.setOutcome(cachedMetricIds);
            }
            catch (final IOException ioe)
            {
                SonargraphLogger.INSTANCE.log(Level.SEVERE, "Failed to read metricIds file '" + getStorageName(), ioe);
                this.cachedMetricIds = null;
                this.lastModified = 0;
                result.addError(ResultCause.IO_EXCEPTION, ioe);
            }
        }
        else
        {
            SonargraphLogger.INSTANCE.log(Level.FINE, "Use cached metricIds");
            result.setOutcome(this.cachedMetricIds);
        }
        return result;
    }

    @Override
    public MetricIds addMetricIds(final MetricIds metricIds, PrintStream logger)
    {
        final ResultWithOutcome<MetricIds> existing = readMetricIds();
        if (existing.isSuccess())
        {
            final MetricIds newMetricIds = new MetricIds();
            final MetricIds existingMetricIds = existing.getOutcome();
            newMetricIds.addAll(existingMetricIds);
            newMetricIds.addAll(metricIds);
            if (!existingMetricIds.equals(newMetricIds))
            {
                SonargraphLogger.logToConsoleOutput(logger, Level.FINE, OVERRIDE_METRIC_IDS_FILE + this.file.getAbsolutePath(), null);
                storeMetricIds(newMetricIds);
            }
            else
            {
                SonargraphLogger.logToConsoleOutput(logger, Level.FINE, ADD_METRIC_IDS_HAD_NO_EFFECT_ON_FILE + this.file.getAbsolutePath(), null);
            }
            return newMetricIds;
        }
        return metricIds;
    }

    private void storeMetricIds(final MetricIds metricIds)
    {
        SonargraphLogger.INSTANCE.log(Level.INFO, "Store {0} metricIds to file {1}",
                new Object[] { metricIds.getMetricIds().size(), this.file.getAbsolutePath() });
        final String jsonString = MetricIds.toJSON(metricIds);
        try (PrintWriter out = new PrintWriter(file, "UTF-8"))
        {
            out.println(jsonString);
        }
        catch (final FileNotFoundException fnfe)
        {
        }
        catch (final UnsupportedEncodingException e)
        {
        }
    }

    @Override
    public String getStorageName()
    {
        return file.getAbsolutePath();
    }
}
