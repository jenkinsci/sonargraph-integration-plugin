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
package com.hello2morrow.sonargraph.integration.jenkins.persistence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

import com.hello2morrow.sonargraph.integration.access.foundation.ResultCause;
import com.hello2morrow.sonargraph.integration.access.foundation.ResultWithOutcome;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.model.IMetricIdsHistoryProvider;

public class MetricIdsHistory implements IMetricIdsHistoryProvider
{

    private final File file;

    public MetricIdsHistory(final File historyFile)
    {
        this.file = historyFile;
        if (!this.file.exists())
        {
            try
            {
                SonargraphLogger.INSTANCE.log(Level.INFO, "Create new empty MetricIds JSON file {0}", this.file.getAbsolutePath());
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
        try
        {
            SonargraphLogger.INSTANCE.log(Level.INFO, "Read metricIds file {0}", this.file.getAbsolutePath());
            jsonString = TextFileReader.readLargeTextFile(this.file);
            result.setOutcome(MetricIds.fromJSON(jsonString));
        }
        catch (final IOException ioe)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Failed to read metricIds file '" + getStorageName(), ioe);
            result.addError(ResultCause.IO_EXCEPTION, ioe);
        }
        return result;
    }

    @Override
    public MetricIds addMetricIds(final MetricIds metricIds)
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
                SonargraphLogger.INSTANCE.log(Level.INFO, "Override metricIds file {0}", this.file.getAbsolutePath());
                storeMetricIds(newMetricIds);
            }
            else
            {
                SonargraphLogger.INSTANCE.log(Level.INFO, "Add metricIds had no effect on file {0}", this.file.getAbsolutePath());
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
