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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.DynaBean;

import com.hello2morrow.sonargraph.integration.access.model.IExportMetaData;
import com.hello2morrow.sonargraph.integration.access.model.IMetricId;
import com.hello2morrow.sonargraph.integration.access.model.IMetricLevel;

import net.sf.json.JSONArray;
import net.sf.json.util.JSONStringer;

public class MetricIds implements Serializable
{
    private static final long serialVersionUID = -5993873338543821075L;
    private final Map<String, MetricId> metricIds = new HashMap<>();

    public void addMetricId(MetricId metricId)
    {
        metricIds.put(metricId.getId(), metricId);
    }

    public Map<String, MetricId> getMetricIds()
    {
        return metricIds;
    }

    public MetricId getMetricId(String metricName)
    {
        return metricIds.get(metricName);
    }

    public Set<String> getMetricCategories()
    {
        final Set<String> result = new HashSet<>();
        for (MetricId metricId : metricIds.values())
        {
            result.addAll(metricId.getCategories());
        }
        return result;
    }

    public Set<MetricId> getMetricIdsForCategory(String metricCategory)
    {
        final Set<MetricId> result = new HashSet<>();
        for (MetricId metricId : metricIds.values())
        {
            if (metricId.getCategories().contains(metricCategory))
            {
                result.add(metricId);
            }
        }
        return result;
    }

    public static String toJSON(MetricIds metricIds)
    {
        JSONStringer builder = new JSONStringer();
        builder.array();
        for (MetricId metricId : metricIds.getMetricIds().values())
        {
            builder.object();
            builder.key("id").value(metricId.getId());
            builder.key("name").value(metricId.getName());
            builder.key("isFloat").value(metricId.isFloat());
            builder.key("categories").array();
            for (String category : metricId.getCategories())
                builder.value(category);
            builder.endArray();
            builder.endObject();
        }
        builder.endArray();
        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    public static MetricIds fromJSON(String jsonString)
    {
        MetricIds result = new MetricIds();
        JSONArray jsonObject = JSONArray.fromObject(jsonString);
        Collection<?> jsonCollection = JSONArray.toCollection(jsonObject);
        for (Object next : jsonCollection)
        {
            DynaBean dynaBean = (DynaBean) next;
            MetricId metricId = new MetricId((String) dynaBean.get("id"), (String) dynaBean.get("name"), (Boolean) dynaBean.get("isFloat"), (List<String>) dynaBean.get("categories"));
            result.addMetricId(metricId);
        }

        return result;
    }
    
    public static MetricIds fromExportMetaData(IExportMetaData exportMetaData)
    {
        MetricIds result = new MetricIds();
        for(IMetricId next : exportMetaData.getMetricIdsForLevel(exportMetaData.getMetricLevels().get(IMetricLevel.SYSTEM)))
        {
            result.addMetricId(MetricId.from(next));
        }
        return result;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((metricIds == null) ? 0 : metricIds.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MetricIds other = (MetricIds) obj;
        if (metricIds == null)
        {
            if (other.metricIds != null)
                return false;
        }
        else if (!metricIds.equals(other.metricIds))
            return false;
        return true;
    }

}
