# MapReduce Jobs for CMM705 Coursework

This folder contains MapReduce jobs for processing weather and location data.

## Structure

```
mapreduce/
├── src/
│   ├── main/
│   │   ├── java/com/rgu/bigdata/
│   │   │   ├── WeatherAnalysisDriver.java    # Main driver
│   │   │   ├── WeatherMapper.java            # Mapper implementation
│   │   │   └── WeatherReducer.java           # Reducer implementation
│   │   └── resources/
│   └── test/
│       └── java/
├── pom.xml                                    # Maven configuration
└── README.md
```

## Building the Project

### Using Maven

```bash
# Clean and build
mvn clean package

# Build without tests
mvn clean package -DskipTests

# Run tests
mvn test
```

The compiled JAR will be in `target/cmm705-coursework-1.0-SNAPSHOT.jar`

## Running MapReduce Jobs

### Local Mode (for testing)

```bash
hadoop jar target/cmm705-coursework-1.0-SNAPSHOT.jar \
    com.rgu.bigdata.WeatherAnalysisDriver \
    input/weatherData.csv \
    output/weather-analysis
```

### On Hadoop Cluster (Docker)

```bash
# Copy JAR to namenode container
docker cp target/cmm705-coursework-1.0-SNAPSHOT.jar namenode:/opt/

# Copy input data to HDFS
docker exec -it namenode hdfs dfs -mkdir -p /input
docker exec -it namenode hdfs dfs -put /opt/data/weatherData.csv /input/

# Run MapReduce job
docker exec -it namenode hadoop jar /opt/cmm705-coursework-1.0-SNAPSHOT.jar \
    com.rgu.bigdata.WeatherAnalysisDriver \
    /input/weatherData.csv \
    /output/weather-analysis

# View results
docker exec -it namenode hdfs dfs -cat /output/weather-analysis/part-r-00000
```

## Data Processing Tasks

### Task 1: Weather Data Analysis

- **Input**: weatherData.csv
- **Processing**: Analyze temperature patterns, rainfall, etc.
- **Output**: Aggregated statistics by location/time

### Task 2: Location Data Processing

- **Input**: locationData.csv
- **Processing**: Geographic analysis and correlations
- **Output**: Location-based insights

## Development Guidelines

### Adding New MapReduce Jobs

1. Create new Mapper class extending `Mapper<KEYIN, VALUEIN, KEYOUT, VALUEOUT>`
2. Create new Reducer class extending `Reducer<KEYIN, VALUEIN, KEYOUT, VALUEOUT>`
3. Create Driver class to configure and run the job
4. Update `pom.xml` if needed for dependencies
5. Build with `mvn package`

### Testing

Use MRUnit for unit testing MapReduce components:

```java
@Test
public void testMapper() throws IOException, InterruptedException {
    MapDriver<LongWritable, Text, Text, IntWritable> mapDriver;
    mapDriver = MapDriver.newMapDriver(new WeatherMapper());

    mapDriver.withInput(new LongWritable(1), new Text("location,temp,date"));
    mapDriver.withOutput(new Text("location"), new IntWritable(1));
    mapDriver.runTest();
}
```

## Debugging

### Enable Debug Logging

Add to `src/main/resources/log4j.properties`:

```properties
log4j.rootLogger=DEBUG, stdout
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss} %-5p %c{1}:%L - %m%n
```

### View Job Logs

```bash
# YARN logs
docker exec -it resourcemanager yarn logs -applicationId <app_id>

# MapReduce job history
docker exec -it namenode hdfs dfs -cat /tmp/hadoop-yarn/staging/history/done/*
```

## Common Issues

### OutOfMemory Errors

Increase memory in `hadoop.env`:

```
YARN_CONF_yarn_nodemanager_resource_memory___mb=16384
MAPRED_CONF_mapreduce_map_memory_mb=4096
MAPRED_CONF_mapreduce_reduce_memory_mb=8192
```

### ClassNotFoundException

Ensure all dependencies are included in the JAR (use maven-shade-plugin)

### Input File Not Found

Verify file exists in HDFS:

```bash
docker exec -it namenode hdfs dfs -ls /input/
```

## Performance Optimization

- Set appropriate number of reducers: `job.setNumReduceTasks(n)`
- Enable compression for intermediate data
- Use Combiners for pre-aggregation
- Optimize input splits for large files

## References

- [Hadoop MapReduce Tutorial](https://hadoop.apache.org/docs/stable/hadoop-mapreduce-client/hadoop-mapreduce-client-core/MapReduceTutorial.html)
- [Maven in 5 Minutes](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)
- [MRUnit Testing Framework](https://mrunit.apache.org/)
