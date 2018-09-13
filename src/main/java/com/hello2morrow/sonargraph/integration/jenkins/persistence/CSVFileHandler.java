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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import com.hello2morrow.sonargraph.integration.access.foundation.Utility;
import com.hello2morrow.sonargraph.integration.access.model.IExportMetaData;
import com.hello2morrow.sonargraph.integration.jenkins.foundation.SonargraphLogger;
import com.hello2morrow.sonargraph.integration.jenkins.model.BuildDataPoint;
import com.hello2morrow.sonargraph.integration.jenkins.model.IDataPoint;
import com.hello2morrow.sonargraph.integration.jenkins.model.IMetricHistoryProvider;
import com.hello2morrow.sonargraph.integration.jenkins.model.InvalidDataPoint;
import com.hello2morrow.sonargraph.integration.jenkins.model.NotExistingDataPoint;
import com.opencsv.CSVReader;

/**
 * Handles operations on a CSV file.
 * @author esteban
 */
public class CSVFileHandler implements IMetricHistoryProvider
{
    public static final char CSV_SEPARATOR = ';';
    
    private static final String BUILDNUMBER_COLUMN_NAME = "buildnumber";
    private static final String TIMESTAMP_COLUMN_NAME = "timestamp";

    private static final String NOT_EXISTING_VALUE = "-";

    private final File m_file;
    private final MetricIds m_metaData;
    private final CSVColumnMapper m_columnMapper;

    public CSVFileHandler(final File csvFile, final IExportMetaData exportMetaData)
    {
        this(csvFile, MetricIds.fromExportMetaData(exportMetaData));
        
    }
    
    public CSVFileHandler(final File csvFile, final MetricIds metaData)
    {
        m_file = csvFile;
        m_metaData = metaData;
        if (!m_file.exists())
        {
            try
            {
                m_file.createNewFile();
                final FileWriter fileWriter = new FileWriter(m_file, true);
                final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write(createHeaderLine());
                bufferedWriter.newLine();
                bufferedWriter.flush();
                bufferedWriter.close();
            }
            catch (final IOException ex)
            {
                SonargraphLogger.INSTANCE.log(Level.SEVERE, "Failed to create CSV file '" + m_file.getAbsolutePath(), ex);
            }
            m_columnMapper = new CSVColumnMapper(m_metaData);
        }
        else
        {
            final String[] existingColumnNames = getHeaderLine();
            m_columnMapper = new CSVColumnMapper(m_metaData, existingColumnNames);
            changeHeaderLine(existingColumnNames, m_columnMapper.getColumnNames(false));
        }
    }

    private void changeHeaderLine(final String[] existingColumnNames, final String[] newColumnNames)
    {
        if (Arrays.equals(existingColumnNames, newColumnNames))
        {
            return;
        }
        final String newHeader = String.join(String.valueOf(CSV_SEPARATOR), newColumnNames);

        Path temp;
        try
        {
            temp = Files.createTempFile("pre", "suff");
            Files.copy(m_file.toPath(), temp, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (final IOException e)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Failed to copy file to change header line'", e);
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(m_file.toPath(), StandardCharsets.US_ASCII);
                BufferedReader reader = Files.newBufferedReader(temp, StandardCharsets.US_ASCII))
        {
            writer.write(newHeader);
            writer.write(Utility.LINE_SEPARATOR);
            reader.readLine(); // NOSONAR ignore old header line

            String line;
            while ((line = reader.readLine()) != null)
            {
                writer.write(line);
                writer.write(Utility.LINE_SEPARATOR);
            }
        }
        catch (final IOException ioe)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Failed to write file to change header line'", ioe);
        }

    }

    private String[] getHeaderLine()
    {

        try (final CSVReader csvReader = new CSVReader(new FileReader(m_file), CSV_SEPARATOR);)
        {
            final String[] nextLine = csvReader.readNext();
            return nextLine;
        }
        catch (final IOException ex)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Failed to get header line from CSV file '" + m_file.getAbsolutePath(), ex);
            return new String[0];
        }
    }

    public String createHeaderLine()
    {
        final StringBuilder headerLine = new StringBuilder(BUILDNUMBER_COLUMN_NAME).append(CSV_SEPARATOR);
        headerLine.append(TIMESTAMP_COLUMN_NAME);

        for (final String metricName : m_metaData.getMetricIds().keySet())
        {
            headerLine.append(CSV_SEPARATOR);
            headerLine.append(metricName);
        }
        return headerLine.toString();
    }

    @Override
    public List<IDataPoint> readMetricValues(final MetricId metric) throws IOException
    {
        final List<IDataPoint> sonargraphDataset = new ArrayList<>();

        if (!m_file.exists())
        {
            return sonargraphDataset;
        }

        try (CSVReader csvReader = new CSVReader(new FileReader(m_file), CSV_SEPARATOR))
        {
            String[] nextLine;
            final int column = m_columnMapper.getIndex(metric.getId());
            csvReader.readNext(); //We do nothing with the header line.
            while ((nextLine = csvReader.readNext()) != null)
            {
                if (nextLine.length == 0 || nextLine.length <= column)
                {
                    //No values contained in line
                    continue;
                }

                processLine(nextLine, column, sonargraphDataset, metric, NumberFormat.getInstance(Locale.US));
            }
        }
        catch (final IOException ioe)
        {
            SonargraphLogger.INSTANCE.log(Level.WARNING, "Exception occurred while reading from file '" + m_file.getAbsolutePath(), ioe);

        }
        return sonargraphDataset;
    }

    protected void processLine(final String[] nextLine, final int column, final List<IDataPoint> sonargraphDataset, final MetricId metric,
            final NumberFormat numberFormat)
    {
        int buildNumber;

        final int indexOfBuildnumber = m_columnMapper.getIndex(BUILDNUMBER_COLUMN_NAME);
        if (indexOfBuildnumber == -1)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Build number could not be found");
            return;
        }
        final String buildNumberString = nextLine[indexOfBuildnumber];
        try
        {
            buildNumber = Integer.parseInt(buildNumberString);
        }
        catch (final NumberFormatException ex)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Build number '" + buildNumberString + "' could not be parsed to an integer value.");
            return;
        }

        long timestamp;
        final String timestampString = nextLine[m_columnMapper.getIndex(TIMESTAMP_COLUMN_NAME)];
        try
        {
            timestamp = Long.parseLong(timestampString);
        }
        catch (final NumberFormatException ex)
        {
            SonargraphLogger.INSTANCE.log(Level.SEVERE, "Timestamp '" + timestampString + "' could not be parsed to a long value.");
            return;
        }

        Number value;
        final String valueString = nextLine[column].trim();
        try
        {
            if (valueString.equals(NOT_EXISTING_VALUE))
            {
                SonargraphLogger.INSTANCE.log(Level.FINE, "Skipping value for metric '" + metric.getName() + "' for build number '"
                        + buildNumberString + "'; it did not exist in Sonargraph XML report.");
                sonargraphDataset.add(new NotExistingDataPoint(buildNumber));
                return;
            }
            value = numberFormat.parse(valueString);
            sonargraphDataset.add(new BuildDataPoint(buildNumber, value.doubleValue(), timestamp));
        }
        catch (final NumberFormatException ex)
        {
            SonargraphLogger.INSTANCE.log(Level.WARNING,
                    "The value of metric '" + metric.getName() + "' for build number '" + buildNumberString + "' is not a valid number. Found '"
                            + valueString + "' but expected a Number. File '" + m_file.getAbsolutePath() + "' might be corrupt:" + "\n"
                            + ex.getMessage());
            sonargraphDataset.add(new InvalidDataPoint(buildNumber));
        }
        catch (final ParseException ex)
        {
            SonargraphLogger.INSTANCE.log(Level.WARNING,
                    "The value of metric '" + metric.getName() + "' for build number '" + nextLine[0] + "' is not a valid number. Found '"
                            + valueString + "' but expected a Number. File '" + m_file.getAbsolutePath() + "' might be corrupt:" + "\n"
                            + ex.getMessage());
            sonargraphDataset.add(new InvalidDataPoint(buildNumber));
        }
        catch (final ArrayIndexOutOfBoundsException ex)
        {
            SonargraphLogger.INSTANCE.log(Level.WARNING, "The value of metric '" + metric.getName() + "' for build number '" + nextLine[0]
                    + "' was not found. File '" + m_file.getAbsolutePath() + "' might be corrupt", ex);
            sonargraphDataset.add(new NotExistingDataPoint(buildNumber));
        }
    }

    @Override
    public void writeMetricValues(final Integer buildnumber, final long timestamp, final Map<MetricId, String> metricValues) throws IOException
    {
        final FileWriter fileWriter = new FileWriter(m_file, true);
        final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        final StringBuilder line = new StringBuilder(buildnumber.toString()).append(CSV_SEPARATOR);

        line.append(timestamp);
        for (final String metric : m_columnMapper.getColumnNames(true))
        {
            line.append(CSV_SEPARATOR);
            final String value = metricValues.get(m_metaData.getMetricIds().get(metric));
            if (value == null)
            {
                line.append(NOT_EXISTING_VALUE);
            }
            else
            {
                line.append(value);
            }
        }
        bufferedWriter.write(line.toString());
        bufferedWriter.newLine();
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    @Override
    public String getStorageName()
    {
        return m_file.getAbsolutePath();
    }

}
