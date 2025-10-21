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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.hello2morrow.sonargraph.integration.access.controller.ControllerFactory;
import com.hello2morrow.sonargraph.integration.access.foundation.ResultWithOutcome;
import com.hello2morrow.sonargraph.integration.access.model.IExportMetaData;
import com.hello2morrow.sonargraph.integration.jenkins.model.BuildDataPoint;
import com.hello2morrow.sonargraph.integration.jenkins.model.IDataPoint;
import com.hello2morrow.sonargraph.integration.jenkins.model.IMetricHistoryProvider;
import com.hello2morrow.sonargraph.integration.jenkins.model.InvalidDataPoint;
import com.hello2morrow.sonargraph.integration.jenkins.model.NotExistingDataPoint;
import com.opencsv.CSVReader;

public class CSVFileHandlerTest
{
    private static final String META_DATA_XML = "src/test/resources/CSVFileHandlerTestMetaData.xml";
    private static final String CSV_FILE_PATH = "src/test/resources/sonargraph.csv";
    private static final String NON_EXISTING_CSV_FILE_NAME = "non-existing.csv";
    private static final String CORRUPT_CSV_FILE_PATH = "src/test/resources/corrupt.csv";

    private static MetricIds s_metaData;

    private final List<BuildDataPoint> referenceDataSet = new ArrayList<>();

    private MetricId m_metric1;
    private MetricId m_metric2;
    private MetricId m_metric3;
    private MetricId m_metric4;
    private MetricId m_metric5;

    @TempDir
    public File folder;

    @BeforeAll
    static void setUpClass() throws IOException
    {
        final File exportMetaDataFile = new File(META_DATA_XML);
        final ResultWithOutcome<IExportMetaData> result = ControllerFactory.createMetaDataController().loadExportMetaData(exportMetaDataFile);
        if (result.isSuccess())
        {
            s_metaData = MetricIds.fromExportMetaData(result.getOutcome());
        }
        else
        {
            throw new IOException("Couldn't load meta data file " + exportMetaDataFile.getAbsolutePath());
        }
    }

    @BeforeEach
    void setUp()
    {
        int buildNumber = 31;
        final double value = 3.0;
        referenceDataSet.add(new BuildDataPoint(buildNumber++, value, 0));
        referenceDataSet.add(new BuildDataPoint(buildNumber++, value, 0));
        referenceDataSet.add(new BuildDataPoint(buildNumber++, value, 0));
        referenceDataSet.add(new BuildDataPoint(buildNumber++, value, 0));
        referenceDataSet.add(new BuildDataPoint(buildNumber++, value, 0));

        m_metric1 = s_metaData.getMetricIds().get("metric1");
        assertNotNull(m_metric1);
        m_metric2 = s_metaData.getMetricIds().get("metric2");
        assertNotNull(m_metric2);
        m_metric3 = s_metaData.getMetricIds().get("metric3");
        assertNotNull(m_metric3);
        m_metric4 = s_metaData.getMetricIds().get("metric4");
        assertNotNull(m_metric4);
        m_metric5 = s_metaData.getMetricIds().get("metric5");
        assertNotNull(m_metric5);
    }

    @Test
    void testCSVFileCreation() throws Exception
    {
        final File newFile = new File(folder, NON_EXISTING_CSV_FILE_NAME);
        final CSVFileHandler handler = new CSVFileHandler(newFile, s_metaData);
        final String shoudBeTheFirstLine = handler.createHeaderLine();

        final CSVReader csvReader = new CSVReaderBuilder(new FileReader(newFile))
                .withCSVParser(
                        new CSVParserBuilder()
                                .withSeparator(CSVFileHandler.CSV_SEPARATOR)
                                .build())
                .build();
        assertArrayEquals(shoudBeTheFirstLine.split(String.valueOf(CSVFileHandler.CSV_SEPARATOR)), csvReader.readNext());
        csvReader.close();
    }

    @Test
    void testReadSonargraphCSVFile() throws IOException
    {
        final File newFile = new File(folder, NON_EXISTING_CSV_FILE_NAME);
        IMetricHistoryProvider csvFileHandler = new CSVFileHandler(newFile, s_metaData);
        List<IDataPoint> testDataset = csvFileHandler.readMetricValues(m_metric1);
        assertEquals(0, testDataset.size());

        csvFileHandler = new CSVFileHandler(new File(CSV_FILE_PATH), s_metaData);
        testDataset = csvFileHandler.readMetricValues(m_metric1);
        assertEquals(5, testDataset.size());
        assertEquals(referenceDataSet, testDataset);
        testDataset = csvFileHandler.readMetricValues(m_metric2);
        assertEquals(5, testDataset.size());
        testDataset = csvFileHandler.readMetricValues(m_metric3);
        assertEquals(0, testDataset.size());
        testDataset = csvFileHandler.readMetricValues(m_metric4);
        assertEquals(0, testDataset.size());
        testDataset = csvFileHandler.readMetricValues(m_metric5);
        assertEquals(0, testDataset.size());
    }

    @Test
    void testReadMetrics() throws IOException
    {
        final File newFile = new File(folder, NON_EXISTING_CSV_FILE_NAME);
        IMetricHistoryProvider csvFileHandler = new CSVFileHandler(newFile, s_metaData);

        List<IDataPoint> dataset = csvFileHandler.readMetricValues(m_metric1);
        assertEquals(0, dataset.size());

        csvFileHandler = new CSVFileHandler(new File(CSV_FILE_PATH), s_metaData);

        dataset = csvFileHandler.readMetricValues(m_metric2);
        assertEquals(5, dataset.size());
        for (final IDataPoint point : dataset)
        {
            assertInstanceOf(InvalidDataPoint.class, point);
        }

        dataset = csvFileHandler.readMetricValues(m_metric1);
        assertEquals(5, dataset.size());
        assertEquals(referenceDataSet, dataset);
    }

    @Test
    void testNoExceptionsExpectedReadingMetrics()
    {
        final File corrupFile = new File(CORRUPT_CSV_FILE_PATH);
        final IMetricHistoryProvider csvFileHandler = new CSVFileHandler(corrupFile, s_metaData);
        assertDoesNotThrow(() -> {
            List<IDataPoint> testDataset = csvFileHandler.readMetricValues(m_metric3);
            assertInstanceOf(InvalidDataPoint.class, testDataset.get(1));
        }, "No exception ParseException should be thrown");

        assertDoesNotThrow(() -> {
            List<IDataPoint> testDataset = csvFileHandler.readMetricValues(m_metric4);
            assertInstanceOf(NotExistingDataPoint.class, testDataset.get(0));
        }, "No exception ArrayIndexOutOfBoundsException should be thrown");
    }

    @Test
    void testWriteMetricsToFile() throws Exception
    {
        final File newFile = new File(folder, NON_EXISTING_CSV_FILE_NAME);
        final IMetricHistoryProvider csvFileHandler = new CSVFileHandler(newFile, s_metaData);

        final HashMap<MetricId, String> buildMetrics = new HashMap<>();
        buildMetrics.put(m_metric1, "2.6");
        buildMetrics.put(m_metric2, "7");
        buildMetrics.put(m_metric3, "3");
        buildMetrics.put(m_metric5, "200.456");
        final long timestamp = System.currentTimeMillis();
        csvFileHandler.writeMetricValues(1, timestamp, buildMetrics);
        final CSVReader csvReader = new CSVReaderBuilder(new FileReader(newFile))
                .withCSVParser(
                        new CSVParserBuilder()
                                .withSeparator(CSVFileHandler.CSV_SEPARATOR)
                                .build())
                .build();
        csvReader.readNext(); //Do nothing with the first line
        final String[] line = csvReader.readNext();
        csvReader.close();
        //1, -, -, 200, -, -, 7, -, 3, -, -, -, -, 2.6
        final String[] expectedLine = { "1", Long.toString(timestamp), "2.6", "7", "3", "-", "200.456" };
        assertArrayEquals(expectedLine, line);
    }
}
