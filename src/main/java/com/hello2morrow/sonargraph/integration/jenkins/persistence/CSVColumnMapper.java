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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.hello2morrow.sonargraph.integration.access.model.IExportMetaData;
import com.hello2morrow.sonargraph.integration.jenkins.model.MetricIds;

/**
 * Maps column names to index of column.
 * There are 2 predefined columns: buildnumber and timestamp.
 * There may be existing column names (from an existing csv file).
 * And there may be additional metric/column names from MetricMetaData.
 *
 * @author andreas
 *
 */
public final class CSVColumnMapper
{
    private final Set<String> m_columnNames = new LinkedHashSet<>();

    public CSVColumnMapper(final IExportMetaData metricMetaData, final String... existingColumnNames)
    {
        this(metricMetaData.getMetricIds().keySet(), existingColumnNames);
    }

    public CSVColumnMapper(final MetricIds metricMetaData, final String... existingColumnNames)
    {
        this(metricMetaData.getMetricIds().keySet(), existingColumnNames);
    }

    private CSVColumnMapper(final Set<String> metricIds, final String... existingColumnNames)
    {
        m_columnNames.add("buildnumber");
        m_columnNames.add("timestamp");

        for (final String columnName : existingColumnNames)
        {
            m_columnNames.add(columnName);
        }
        m_columnNames.addAll(metricIds);
    }

    public int getIndex(final String columnName)
    {
        if (!m_columnNames.contains(columnName))
        {
            return -1;
        }
        final List<String> list = new ArrayList<>(m_columnNames);
        return list.indexOf(columnName);
    }

    public String[] getColumnNames(final boolean metricOnly)
    {
        if (metricOnly)
        {
            return m_columnNames.stream().skip(2).toArray(size -> new String[size]);
        }
        return m_columnNames.stream().toArray(size -> new String[size]);
    }
}