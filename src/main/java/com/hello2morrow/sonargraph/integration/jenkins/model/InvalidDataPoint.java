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
package com.hello2morrow.sonargraph.integration.jenkins.model;

public class InvalidDataPoint implements IDataPoint
{
    private final int m_x;

    public InvalidDataPoint(final int x)
    {
        m_x = x;
    }

    @Override
    public int getX()
    {
        return m_x;
    }

    @Override
    public double getY()
    {
        return Double.NaN;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + m_x;
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
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final InvalidDataPoint other = (InvalidDataPoint) obj;
        if (m_x != other.m_x)
        {
            return false;
        }
        return true;
    }

}
