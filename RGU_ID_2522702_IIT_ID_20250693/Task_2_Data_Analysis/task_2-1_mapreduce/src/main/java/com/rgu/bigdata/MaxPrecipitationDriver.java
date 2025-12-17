package com.rgu.bigdata;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * Driver for Task 2.1.2: Find the month and year with the highest total
 * precipitation
 * This job aggregates precipitation data by month and identifies the month
 * with the maximum total precipitation across all districts in Sri Lanka.
 */
public class MaxPrecipitationDriver extends Configured implements Tool {

    @Override
    public int run(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: MaxPrecipitationDriver <input_path> <output_path>");
            System.err.println("Example: hadoop jar bigdata-weather-1.0.jar " +
                    "com.rgu.bigdata.MaxPrecipitationDriver " +
                    "/data/weatherData.csv /output/task2_1_2");
            return -1;
        }

        Configuration conf = getConf();
        Job job = Job.getInstance(conf, "Task 2.1.2 - Maximum Precipitation Month");

        job.setJarByClass(MaxPrecipitationDriver.class);
        job.setMapperClass(MaxPrecipitationMapper.class);
        job.setReducerClass(MaxPrecipitationReducer.class);

        // Output key/value types
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        // Input/output paths
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        // Set number of reducers to 1 to ensure global maximum is found
        job.setNumReduceTasks(1);

        System.out.println("============================================");
        System.out.println("Task 2.1.2: Maximum Precipitation Analysis");
        System.out.println("============================================");
        System.out.println("Input Path: " + args[0]);
        System.out.println("Output Path: " + args[1]);
        System.out.println("Mapper: MaxPrecipitationMapper");
        System.out.println("Reducer: MaxPrecipitationReducer");
        System.out.println("============================================");

        boolean success = job.waitForCompletion(true);

        if (success) {
            System.out.println("\n✓ Job completed successfully!");
            System.out.println("Check output at: " + args[1]);
            System.out.println("Look for '==== MAXIMUM PRECIPITATION MONTH ====' in the output file");
        } else {
            System.err.println("\n✗ Job failed!");
        }

        return success ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Configuration(), new MaxPrecipitationDriver(), args);
        System.exit(exitCode);
    }
}
