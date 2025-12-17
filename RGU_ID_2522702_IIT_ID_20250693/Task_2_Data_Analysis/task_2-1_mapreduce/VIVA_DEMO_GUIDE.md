# VIVA DEMONSTRATION GUIDE - Task 1 MapReduce

## Overview
This guide will help you demonstrate your MapReduce implementation during the viva.

---

## Pre-Viva Checklist
- [ ] Hadoop cluster is running
- [ ] Datasets are available
- [ ] Java and Maven are installed
- [ ] You understand the code logic

---

## DEMO SCRIPT (Follow in Order)

### Step 1: Show Your Project Structure
```bash
cd /home/iitgcpuser/CMM705---Big-Data-Programming---Coursework/coursework/MyWork/Task\ 1
ls -la
```

**Explain:** 
- `env_setup/` - Docker configuration for Hadoop cluster
- `task_1_mapreduce/` - MapReduce Java code
- Output files with results

---

### Step 2: Show Your Java Code
```bash
cd task_1_mapreduce/src/main/java/com/rgu/bigdata
ls -la
```

**Explain:**
- **Task 1 (Monthly Analysis):** 3 files
  - `MonthlyWeatherDriver.java` - Configures and launches the job
  - `MonthlyWeatherMapper.java` - Extracts location, month, year, precipitation, temperature
  - `MonthlyWeatherReducer.java` - Aggregates data per district per month
  
- **Task 2 (Max Precipitation):** 3 files
  - `MaxPrecipitationDriver.java` - Configures and launches the job
  - `MaxPrecipitationMapper.java` - Extracts month, year, precipitation
  - `MaxPrecipitationReducer.java` - Finds month with highest precipitation

---

### Step 3: Verify Hadoop Cluster is Running
```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

**Expected Output:**
- namenode (running)
- datanode1, datanode2 (running)
- resourcemanager (running)
- nodemanager1, nodemanager2 (running)

**If not running:**
```bash
cd ../env_setup
docker-compose up -d
sleep 30
```

---

### Step 4: Build the MapReduce JAR
```bash
cd /home/iitgcpuser/CMM705---Big-Data-Programming---Coursework/coursework/MyWork/Task\ 1/task_1_mapreduce
mvn clean package -DskipTests
```

**Explain:**
- Maven compiles Java code
- Creates JAR file with all dependencies
- JAR location: `target/cmm705-coursework-1.0-SNAPSHOT.jar`

**Verify:**
```bash
ls -lh target/cmm705-coursework-1.0-SNAPSHOT.jar
```

---

### Step 5: Upload Data to HDFS
```bash
# Copy datasets to namenode container
docker cp /home/iitgcpuser/CMM705---Big-Data-Programming---Coursework/Datasets/weatherData.csv namenode:/tmp/
docker cp /home/iitgcpuser/CMM705---Big-Data-Programming---Coursework/Datasets/locationData.csv namenode:/tmp/

# Upload to HDFS
docker exec namenode hdfs dfs -mkdir -p /input
docker exec namenode hdfs dfs -put -f /tmp/weatherData.csv /input/
docker exec namenode hdfs dfs -put -f /tmp/locationData.csv /input/

# Verify upload
docker exec namenode hdfs dfs -ls /input/
docker exec namenode hdfs dfs -du -h /input/
```

**Explain:**
- weatherData.csv contains weather records from 2010-2024 (142K+ records)
- locationData.csv contains city names for 27 districts (loaded dynamically by reducer)
- Data is distributed across HDFS blocks
- Replicated for fault tolerance (default replication factor = 3)

---

### Step 6: Copy JAR to Namenode
```bash
docker cp target/cmm705-coursework-1.0-SNAPSHOT.jar namenode:/tmp/
```

---

### Step 7: Run Task 1 - Monthly Weather Analysis
```bash
# Run the MapReduce job
docker exec namenode bash -c "export HADOOP_CLASSPATH=/tmp/cmm705-coursework-1.0-SNAPSHOT.jar && hadoop com.rgu.bigdata.MonthlyWeatherDriver /input/weatherData.csv /output/task1_demo"
```

**What to explain while it runs:**
1. **Map Phase:**
   - Mapper reads weatherData.csv
   - Filters records from 2014-2024 (past decade)
   - Extracts: location_id, month, year, precipitation_hours, temperature_2m_mean
   - Emits key: "locationId-month-year", value: "precipitation,temperature,count"

2. **Shuffle & Sort Phase:**
   - Hadoop groups all values by key
   - Data is sorted automatically

3. **Reduce Phase:**
   - Reducer's setup() method loads location names from locationData.csv (runs once)
   - Reducer receives grouped data
   - Sums total precipitation per district per month
   - Calculates mean temperature
   - Stores results in ArrayList
   - cleanup() method sorts by year (descending) - most recent first
   - Outputs human-readable sentences

**Wait for:** "Loaded 27 locations from locationData.csv" and "Job completed successfully"

---

### Step 8: View Task 1 Results
```bash
# Download results
docker exec namenode hdfs dfs -get /output/task1_demo/part-r-00000 /tmp/task1_output.txt
docker cp namenode:/tmp/task1_output.txt ./

# Show first 20 lines
head -20 task1_output.txt
```

**Expected Output Format:**
```
Colombo had a total precipitation of 138 hours with a mean temperature of 26 for 1st month in 2024
Colombo had a total precipitation of 38 hours with a mean temperature of 28 for 2nd month in 2024
...
```

**Explain:**
- Sorted by most recent year (2024) first
- All 27 districts included
- Data covers past decade (2014-2024)

---

### Step 9: Run Task 2 - Maximum Precipitation
```bash
# Run the MapReduce job
docker exec namenode bash -c "export HADOOP_CLASSPATH=/tmp/cmm705-coursework-1.0-SNAPSHOT.jar && hadoop com.rgu.bigdata.MaxPrecipitationDriver /input/weatherData.csv /output/task2_demo"
```

**What to explain while it runs:**
1. **Map Phase:**
   - Mapper reads ALL weatherData.csv records (2010-2024)
   - Extracts: month, year, precipitation_hours
   - Emits key: "month-year", value: "precipitation"

2. **Reduce Phase:**
   - Reducer sums precipitation for each month-year
   - Tracks global maximum
   - Outputs the month with highest total precipitation

**Wait for:** "Job completed successfully"

---

### Step 10: View Task 2 Results
```bash
# Download results
docker exec namenode hdfs dfs -get /output/task2_demo/part-r-00000 /tmp/task2_output.txt
docker cp namenode:/tmp/task2_output.txt ./

# Show the result (last line)
tail -1 task2_output.txt
```

**Expected Output:**
```
12th month in 2014 had the highest total precipitation of 14365 hours
```

**Explain:**
- December 2014 had the highest precipitation
- Analyzed full dataset (2010-2024)
- Total of 14,365 hours across all 27 districts

---

## Understanding Questions You Might Be Asked

### Q1: Why use MapReduce for this problem?
**Answer:** 
- Large dataset (142K+ records)
- Needs parallel processing
- Map phase distributes computation
- Reduce phase aggregates results
- Scalable - can handle even larger datasets

### Q2: Explain the key-value pairs in Task 1
**Answer:**
- **Mapper output:** Key="0-1-2024", Value="5.5,26.3,1"
- Key combines location, month, year
- Value has precipitation, temperature, count
- **Reducer output:** Aggregates by key, outputs sentence

### Q3: Why filter for 2014-2024 in Task 1 but not Task 2?
**Answer:**
- Task 1 requirement: "past decade" (10 years)
- Task 2 requirement: "full dataset"
- Filtering done in mapper logic

### Q4: How does sorting work?
**Answer:**
- Reducer stores all results in ArrayList
- In cleanup() method, sorts using Comparator
- Sorted by: year (descending), then location, then month
- Outputs sorted results

### Q5: How do you load city names dynamically?
**Answer:**
- Reducer's setup() method runs once before reduce tasks
- Reads locationData.csv from HDFS using FileSystem API
- Parses CSV and builds HashMap (location_id -> city_name)
- Maps location_id (0-26) to city names dynamically
- This is better than hardcoding - separates data from code
- Makes it maintainable and scalable

### Q6: Why load locationData.csv in the reducer, not the mapper?
**Answer:**
- Only the reducer needs city names for output formatting
- Mapper only works with location_id numbers
- Loading once per reducer task is efficient
- Reduces memory overhead on mapper tasks
- Follows best practice of doing work where it's needed

---

## Quick Commands Reference

### Check Hadoop Status
```bash
docker exec namenode hdfs dfsadmin -report
```

### View HDFS Files
```bash
docker exec namenode hdfs dfs -ls /
docker exec namenode hdfs dfs -ls /input/
docker exec namenode hdfs dfs -ls /output/
```

### View YARN Jobs
```bash
# Access YARN UI: http://localhost:8088
# Access HDFS UI: http://localhost:9870
```

### Clean Up Old Outputs
```bash
docker exec namenode hdfs dfs -rm -r /output/task1_demo
docker exec namenode hdfs dfs -rm -r /output/task2_demo
```

### Stop/Start Hadoop
```bash
cd /home/iitgcpuser/CMM705---Big-Data-Programming---Coursework/coursework/MyWork/Task\ 1/env_setup
docker-compose down
docker-compose up -d
```

---

## Troubleshooting

### If job fails:
1. Check YARN logs: `docker logs resourcemanager`
2. Check namenode: `docker logs namenode`
3. Verify data in HDFS: `docker exec namenode hdfs dfs -ls /input/`

### If "Connection refused":
- Wait 30 seconds for services to start
- Check: `docker ps` - ensure all containers are healthy

### If output is empty:
- Check mapper filtering logic
- Verify CSV column indices
- Test with smaller dataset

---

## Key Points to Remember

✅ **Task 1 (Monthly Analysis):**
- Past decade only (2014-2024)
- Groups by district, month, year
- Calculates total precipitation + mean temperature
- Sorted by most recent year first

✅ **Task 2 (Max Precipitation):**
- Full dataset (2010-2024)
- Groups by month-year
- Finds global maximum
- Result: December 2014 with 14,365 hours

✅ **MapReduce Benefits:**
- Parallel processing across multiple nodes
- Scalability - handles large datasets efficiently
- Fault tolerance - automatic recovery from failures
- Data locality - computation moves to data

✅ **Code Quality:**
- Dynamic loading of location data from CSV
- Separation of data and code
- Proper error handling
- Clean, maintainable code structure

---

## Practice Run Before Viva

Do this complete run once before your viva:
1. Stop and restart Hadoop cluster
2. Build JAR from scratch
3. Upload data to HDFS
4. Run both tasks
5. Explain each step out loud
6. Time yourself (should take ~5-7 minutes)

Good luck with your viva! 🎓
