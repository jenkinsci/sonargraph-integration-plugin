/*
 * Jenkins Sonargraph Integration Plugin
 * Copyright (C) 2015-2020 hello2morrow GmbH
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import com.hello2morrow.sonargraph.integration.access.controller.ControllerFactory;
import com.hello2morrow.sonargraph.integration.access.controller.IMetaDataController;
import com.hello2morrow.sonargraph.integration.access.foundation.ResultWithOutcome;
import com.hello2morrow.sonargraph.integration.access.model.IExportMetaData;
import com.hello2morrow.sonargraph.integration.access.model.ISingleExportMetaData;

public class CSVColumnMapperTest
{
    private static final String META_DATA_XML = "src/test/resources/CSVColumnMapperTestMetaData.xml";
    private static IExportMetaData s_metaData;

    @BeforeClass
    public static void setUp() throws IOException
    {
        final IMetaDataController metaDataController = ControllerFactory.createMetaDataController();
        final File metaDataFile = new File(META_DATA_XML);
        final ResultWithOutcome<IExportMetaData> result = metaDataController.loadExportMetaData(metaDataFile);
        if (result.isSuccess())
        {
            s_metaData = result.getOutcome();
        }
        else
        {
            throw new IOException("Couldn't load meta data file " + metaDataFile.getAbsolutePath());
        }
    }

    @Test
    public void testEmptyColumnMapping()
    {
        final ISingleExportMetaData metaData = ISingleExportMetaData.EMPTY;
        final CSVColumnMapper mapper = new CSVColumnMapper(metaData);
        assertArrayEquals(new String[] { "buildnumber", "timestamp" }, mapper.getColumnNames(false));
        // buildnumber and timestamp are the first two columns
        assertEquals(0, mapper.getIndex("buildnumber"));
        assertEquals(1, mapper.getIndex("timestamp"));
    }

    @Test
    public void testColumnMappingFromMetricMetaData()
    {
        final CSVColumnMapper mapper = new CSVColumnMapper(s_metaData);
        assertEquals(5, mapper.getColumnNames(false).length);
        // buildnumber and timestamp are the first two columns
        assertEquals(0, mapper.getIndex("buildnumber"));
        assertEquals(1, mapper.getIndex("timestamp"));
        // metrics from metricMetaData are unordered, so just test if they are there
        assertNotEquals(-1, mapper.getIndex("first"));
        assertNotEquals(-1, mapper.getIndex("second"));
        assertNotEquals(-1, mapper.getIndex("third"));
        // no metric "fourth"
        assertEquals(-1, mapper.getIndex("fourth"));
    }

    @Test
    public void testColumnMappingFromMetricMetaDataAndExisting()
    {
        final CSVColumnMapper mapper = new CSVColumnMapper(s_metaData, "green", "blue", "red");
        assertEquals(8, mapper.getColumnNames(false).length);
        // buildnumber and timestamp are the first two columns
        assertEquals(0, mapper.getIndex("buildnumber"));
        assertEquals(1, mapper.getIndex("timestamp"));
        // existing metrics are ordered
        assertEquals(2, mapper.getIndex("green"));
        assertEquals(3, mapper.getIndex("blue"));
        assertEquals(4, mapper.getIndex("red"));
        // metrics from metricMetaData are unordered, so just test if they are there
        assertNotEquals(-1, mapper.getIndex("first"));
        assertNotEquals(-1, mapper.getIndex("second"));
        assertNotEquals(-1, mapper.getIndex("third"));
        // no metric "fourth"
        assertEquals(-1, mapper.getIndex("fourth"));
    }

    @Test
    public void testColumnMappingFromMetricMetaDataAndExistingMixed()
    {

        final CSVColumnMapper mapper = new CSVColumnMapper(s_metaData, "green", "third", "blue", "first", "red");
        assertEquals(8, mapper.getColumnNames(false).length);
        // buildnumber and timestamp are the first two columns
        assertEquals(0, mapper.getIndex("buildnumber"));
        assertEquals(1, mapper.getIndex("timestamp"));
        // existing metrics are ordered
        assertEquals(2, mapper.getIndex("green"));
        assertEquals(3, mapper.getIndex("third"));
        assertEquals(4, mapper.getIndex("blue"));
        assertEquals(5, mapper.getIndex("first"));
        assertEquals(6, mapper.getIndex("red"));
        // metrics from metricMetaData are unordered, but in this case there is only one left: second
        // because first and third are also contained in existing metrics
        assertEquals(7, mapper.getIndex("second"));
    }

}
