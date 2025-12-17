package com.rgu.bigdata;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Task 2.1.1: Calculate total precipitation and mean temperature per district
 * per month over the past decade (2014-2024)
 * 
 * Input CSV format:
 * location_id,date,weather_code,temp_max,temp_min,temperature_2m_mean,...,precipitation_hours,...
 * Output Key: location_id-month (e.g., "0-1" for Colombo, January - aggregated
 * across all years)
 * Output Value: precipitation_hours,temperature_2m_mean,1 (count)
 */
public class MonthlyWeatherMapper extends Mapper<LongWritable, Text, Text, Text> {

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
            String temperatureMean = fields[columnIndexMap.get("temperature_2m_mean (°C)")].trim();
            String precipitationHours = fields[columnIndexMap.get("precipitation_hours (h)")].trim();

            // Filter out Welimada (location_id=25) and Bandarawela (location_id=26)
            if (locationId.equals("25") || locationId.equals("26")) {
                return; // Skip these cities as they are not districts
            }

            // Parse date to extract month and year
            String[] dateParts = dateStr.split("/");
            if (dateParts.length == 3) {
                String month = dateParts[0]; // Month
                String year = dateParts[2]; // Year

                // Filter for past decade only (2014-2024)
                int yearInt = Integer.parseInt(year);
                if (yearInt < 2014) {
                    return; // Skip records before 2014
                }

                // Create key: locationId-month (WITHOUT year to aggregate across all years)
                String keyStr = locationId + "-" + month;
                outputKey.set(keyStr);

                // Create value: precipitation,temperature,count
                String valueStr = precipitationHours + "," + temperatureMean + ",1";
                outputValue.set(valueStr);

                context.write(outputKey, outputValue);
            }

        } catch (Exception e) {
            // Log and skip malformed records
            System.err.println("Error processing line: " + line);
            System.err.println("Error: " + e.getMessage());
        }

    }
}
