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
 * Driver for Task 2.1.1: Monthly precipitation and temperature analysis
 */
public class MonthlyWeatherDriver extends Configured implements Tool {

    @Override
    public int run(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: MonthlyWeatherDriver <input path> <output path>");
            System.err.println(
                    "Example: hadoop jar yourjar.jar com.rgu.bigdata.MonthlyWeatherDriver /input/weather.csv /output/monthly");
            return -1;
        }

        Configuration conf = getConf();
        Job job = Job.getInstance(conf, "Monthly Weather Analysis - Sri Lanka");

        job.setJarByClass(MonthlyWeatherDriver.class);

        // Set Mapper and Reducer
        job.setMapperClass(MonthlyWeatherMapper.class);
        job.setReducerClass(MonthlyWeatherReducer.class);

        // Set output types
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        // Set input and output paths
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        boolean success = job.waitForCompletion(true);
        return success ? 0 : 1;
    }

    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new Configuration(), new MonthlyWeatherDriver(), args);
        System.exit(exitCode);
    }
}
