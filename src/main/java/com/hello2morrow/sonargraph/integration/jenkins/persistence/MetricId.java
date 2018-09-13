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

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.hello2morrow.sonargraph.integration.access.model.IMetricId;

public class MetricId implements Serializable
{
    private static final long serialVersionUID = 4950947267793381336L;

    private final String id;
    private final String name;
    private final boolean isFloat;
    private final Set<String> m_categories = new HashSet<>();

    public MetricId(final String id, final String name, final boolean isFloat, final String... categories)
    {
        this.id = id;
        this.name = name;
        this.isFloat = isFloat;
        if (categories != null && categories.length > 0)
        {
            for (final String category : categories)
            {
                m_categories.add(category);
            }
        }
    }

    public MetricId(final String id, final String name, final boolean isFloat, final List<String> categories)
    {
        this.id = id;
        this.name = name;
        this.isFloat = isFloat;
        if (categories != null)
        {
            m_categories.addAll(categories);
        }
    }

    public Set<String> getCategories()
    {
        return m_categories;
    }

    public String getName()
    {
        return name;
    }

    public String getId()
    {
        return id;
    }

    public boolean isFloat()
    {
        return isFloat;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_categories == null) ? 0 : m_categories.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + (isFloat ? 1231 : 1237);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof MetricId))
        {
            return false;
        }
        final MetricId other = (MetricId) obj;
        if (m_categories == null)
        {
            if (other.m_categories != null)
            {
                return false;
            }
        }
        else if (!m_categories.equals(other.m_categories))
        {
            return false;
        }
        if (id == null)
        {
            if (other.id != null)
            {
                return false;
            }
        }
        else if (!id.equals(other.id))
        {
            return false;
        }
        if (isFloat != other.isFloat)
        {
            return false;
        }
        if (name == null)
        {
            if (other.name != null)
            {
                return false;
            }
        }
        else if (!name.equals(other.name))
        {
            return false;
        }
        return true;
    }

    public static MetricId from(final IMetricId metricId)
    {
        final List<String> categories = metricId.getCategories().stream().map(x -> x.getPresentationName()).collect(Collectors.toList());
        final MetricId result = new MetricId(metricId.getName(), metricId.getPresentationName(), metricId.isFloat(), categories);
        return result;
    }

}
