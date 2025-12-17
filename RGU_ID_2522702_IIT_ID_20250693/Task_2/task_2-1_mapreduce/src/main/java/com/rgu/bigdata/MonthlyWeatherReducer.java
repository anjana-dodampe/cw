package com.rgu.bigdata;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.*;

/**
 * Task 2.1.1: Reducer for calculating total precipitation and mean temperature
 * 
 * Input Key: location_id-month (aggregated across all years 2014-2024)
 * Input Value: precipitation,temperature,count
 * Output: LocationName-Month TAB total_precipitation_hours,mean_temperature
 */
public class MonthlyWeatherReducer extends Reducer<Text, Text, Text, Text> {

    private Text outputKey = new Text();
    private Text result = new Text();

    // Store all results for sorting
    private List<OutputRecord> outputRecords = new ArrayList<>();

    // Inner class to hold output data
    private static class OutputRecord {
        String locationId;
        int month;
        String cityName;
        double totalPrecipitation;
        double meanTemperature;

        OutputRecord(String locationId, int month, String cityName,
                double totalPrecipitation, double meanTemperature) {
            this.locationId = locationId;
            this.month = month;
            this.cityName = cityName;
            this.totalPrecipitation = totalPrecipitation;
            this.meanTemperature = meanTemperature;
        }
    }

    // Map location_id to city name (loaded dynamically from locationData.csv)
    private Map<String, String> locationMap = new HashMap<>();

    @Override
    protected void setup(Context context) throws IOException, InterruptedException {
        super.setup(context);
        loadLocationData(context);
    }

    /**
     * Load location names from locationData.csv in HDFS
     */
    private void loadLocationData(Context context) throws IOException {
        Configuration conf = context.getConfiguration();
        Path locationPath = new Path("/input/locationData.csv");
        FileSystem fs = FileSystem.get(conf);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(locationPath)))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue; // Skip header line
                }

                String[] fields = line.split(",");
                if (fields.length >= 8) {
                    String locationId = fields[0].trim();
                    String cityName = fields[7].trim();
                    locationMap.put(locationId, cityName);
                }
            }
            System.out.println("Loaded " + locationMap.size() + " locations from locationData.csv");
        } catch (IOException e) {
            System.err.println("Error reading locationData.csv: " + e.getMessage());
            throw e;
        }
    }

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        double totalPrecipitation = 0.0;
        double totalTemperature = 0.0;
        int count = 0;

        // Aggregate values for this location-month-year
        for (Text value : values) {
            String[] parts = value.toString().split(",");

            if (parts.length == 3) {
                try {
                    double precip = Double.parseDouble(parts[0]);
                    double temp = Double.parseDouble(parts[1]);
                    int recordCount = Integer.parseInt(parts[2]);

                    totalPrecipitation += precip;
                    totalTemperature += temp;
                    count += recordCount;
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing values: " + value.toString());
                }
            }
        }

        // Calculate mean temperature
        double meanTemperature = count > 0 ? totalTemperature / count : 0.0;

        // Parse key to get location_id and month (NO year - aggregated across all
        // years)
        String[] keyParts = key.toString().split("-");
        if (keyParts.length == 2) {
            String locationId = keyParts[0];
            int month = Integer.parseInt(keyParts[1]);

            // Get city name from map (loaded from locationData.csv)
            String cityName = locationMap.getOrDefault(locationId, "Location_" + locationId);

            // Store the record for later sorting
            outputRecords.add(new OutputRecord(locationId, month, cityName,
                    totalPrecipitation, meanTemperature));
        }
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        // Sort by city name, then by month
        Collections.sort(outputRecords, new Comparator<OutputRecord>() {
            @Override
            public int compare(OutputRecord o1, OutputRecord o2) {
                // First compare by city name
                int cityCompare = o1.cityName.compareTo(o2.cityName);
                if (cityCompare != 0)
                    return cityCompare;

                // Then by month
                return Integer.compare(o1.month, o2.month);
            }
        });

        // Output all sorted records
        for (OutputRecord record : outputRecords) {
            String output = String.format(
                    "%s had a total precipitation of %.0f hours with a mean temperature of %.0f for %s month",
                    record.cityName, record.totalPrecipitation, record.meanTemperature,
                    getOrdinal(record.month));

            outputKey.set("");
            result.set(output);
            context.write(outputKey, result);
        }
    }

    /**
     * Helper method to convert month number to ordinal (1st, 2nd, 3rd, etc.)
     */
    private String getOrdinal(int month) {
        if (month >= 11 && month <= 13) {
            return month + "th";
        }
        switch (month % 10) {
            case 1:
                return month + "st";
            case 2:
                return month + "nd";
            case 3:
                return month + "rd";
            default:
                return month + "th";
        }
    }
}
