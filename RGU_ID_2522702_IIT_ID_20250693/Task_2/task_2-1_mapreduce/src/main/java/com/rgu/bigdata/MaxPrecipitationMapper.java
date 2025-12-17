package com.rgu.bigdata;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapper for Task 2.1.2: Find the month and year with the highest total
 * precipitation
 * 
 * Input CSV format:
 * location_id,date,weather_code,...,precipitation_hours(12),...
 * Output: Key = "Month-Year" (e.g., "2-2019"), Value = precipitation_hours
 * 
 * This mapper extracts the month-year and precipitation from each record,
 * allowing the reducer to find the month with maximum total precipitation.
 */
public class MaxPrecipitationMapper extends Mapper<LongWritable, Text, Text, Text> {

    private Text outputKey = new Text();
    private Text outputValue = new Text();
    private Map<String, Integer> columnIndexMap = new HashMap<>();
    private boolean headerProcessed = false;

    @Override
    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        String line = value.toString();
        String[] fields = line.split(",");

        // Process header to build column index map
        if (key.get() == 0) {
            for (int i = 0; i < fields.length; i++) {
                columnIndexMap.put(fields[i].trim(), i);
            }
            headerProcessed = true;
            return;
        }

        if (!headerProcessed) {
            return; // Safety check
        }

        try {
            // Extract fields by column name
            String locationId = fields[columnIndexMap.get("location_id")].trim();
            String dateStr = fields[columnIndexMap.get("date")].trim();
            String precipitationHours = fields[columnIndexMap.get("precipitation_hours (h)")].trim();

            // Parse date to extract month and year
            String[] dateParts = dateStr.split("/");
            if (dateParts.length == 3) {
                String month = dateParts[0]; // Month
                String year = dateParts[2]; // Year

                // Create key: month-year
                String yearMonth = month + "-" + year;
                outputKey.set(yearMonth);

                // Emit precipitation value
                outputValue.set(precipitationHours);
                context.write(outputKey, outputValue);
            }
        } catch (Exception e) {
            // Skip records with parsing errors
            System.err.println("Error parsing line: " + line);
        }
    }
}
