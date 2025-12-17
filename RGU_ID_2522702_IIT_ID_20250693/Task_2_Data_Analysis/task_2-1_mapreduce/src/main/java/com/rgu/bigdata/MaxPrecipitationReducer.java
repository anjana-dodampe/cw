package com.rgu.bigdata;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * Reducer for Task 2.1.2: Find the month and year with the highest total
 * precipitation
 * This reducer aggregates precipitation for each month and tracks the maximum.
 */
public class MaxPrecipitationReducer extends Reducer<Text, Text, Text, Text> {

    // Global variable to track maximum precipitation month
    private double globalMaxPrecipitation = Double.MIN_VALUE;
    private String maxMonth = "";

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {

        double totalPrecipitation = 0.0;
        int count = 0;

        // Aggregate precipitation for this month-year
        for (Text value : values) {
            try {
                double precipitation = Double.parseDouble(value.toString());
                totalPrecipitation += precipitation;
                count++;
            } catch (NumberFormatException e) {
                System.err.println("Error parsing precipitation value: " + value.toString());
            }
        }

        // Check if this is the global maximum
        if (totalPrecipitation > globalMaxPrecipitation) {
            globalMaxPrecipitation = totalPrecipitation;
            maxMonth = key.toString();
        }

        // Emit all months with their totals
        String output = String.format("Total Precipitation: %.2f hours (from %d records)",
                totalPrecipitation, count);
        // context.write(key, new Text(output));
    }

    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        // Emit the maximum month summary at the end
        if (!maxMonth.isEmpty()) {
            String[] parts = maxMonth.split("-");
            String month = parts[0];
            String year = parts[1];

            // Format: "2nd month in 2019 had the highest total precipitation of 300 hr"
            String result = String.format("%s month in %s had the highest total precipitation of %.0f hours",
                    getOrdinal(Integer.parseInt(month)), year, globalMaxPrecipitation);

            context.write(new Text(""), new Text(result));
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
