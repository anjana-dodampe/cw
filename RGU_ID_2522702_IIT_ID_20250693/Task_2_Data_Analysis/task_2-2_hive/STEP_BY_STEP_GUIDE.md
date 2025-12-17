# Step-by-Step Manual Guide for Hive Weather Analytics
## Task 2: Temperate Cities & Seasonal Evapotranspiration Analysis

Following the lab session pattern - execute each command manually to understand the process.

---

## STEP 1: Verify Docker Containers are Running

```bash
# Check all containers are up
sudo docker ps

# You should see: namenode, datanode1, datanode2, hive-server, hive-metastore
```

---

## STEP 2: Prepare Data in Namenode Container

### 2.1: Copy datasets to namenode
```bash
# Copy location data
sudo docker cp /home/iitgcpuser/CMM705---Big-Data-Programming---Coursework/Datasets/locationData.csv namenode:/tmp/

# Copy weather data
sudo docker cp /home/iitgcpuser/CMM705---Big-Data-Programming---Coursework/Datasets/weatherData.csv namenode:/tmp/
```

### 2.2: Access namenode and create HDFS directories
```bash
# Enter namenode container
sudo docker exec -it namenode bash

# Inside namenode, create directories for data
hdfs dfs -mkdir -p /user/hive/warehouse/weather_analytics.db/location
hdfs dfs -mkdir -p /user/hive/warehouse/weather_analytics.db/weather

# Upload data to HDFS
hdfs dfs -put /tmp/locationData.csv /user/hive/warehouse/weather_analytics.db/location/
hdfs dfs -put /tmp/weatherData.csv /user/hive/warehouse/weather_analytics.db/weather/

# Verify data uploaded
hdfs dfs -ls /user/hive/warehouse/weather_analytics.db/location
hdfs dfs -ls /user/hive/warehouse/weather_analytics.db/weather

# View first few lines of uploaded data
hdfs dfs -cat /user/hive/warehouse/weather_analytics.db/location/locationData.csv | head -5
hdfs dfs -cat /user/hive/warehouse/weather_analytics.db/weather/weatherData.csv | head -5

# Exit namenode
exit
```

---

## STEP 3: Create Hive Database and Tables

### 3.1: Access hive-server container
```bash
sudo docker exec -it hive-server bash
```

### 3.2: Start Hive shell
```bash
hive
```

### 3.3: Create database
```sql
-- Create database
CREATE DATABASE IF NOT EXISTS weather_analytics;

-- Show all databases
SHOW DATABASES;

-- Use the database
USE weather_analytics;

-- Verify you're in the correct database
SELECT current_database();
```

### 3.4: Create Location Table
```sql
-- Drop table if exists for fresh start
DROP TABLE IF EXISTS location_data;

-- Create managed table for location data
CREATE TABLE location_data (
    location_id INT,
    latitude DOUBLE,
    longitude DOUBLE,
    elevation INT,
    utc_offset_seconds INT,
    timezone STRING,
    timezone_abbreviation STRING,
    city_name STRING
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
TBLPROPERTIES ('skip.header.line.count'='1');

-- Load data into the table
LOAD DATA INPATH '/user/hive/warehouse/weather_analytics.db/location/locationData.csv' INTO TABLE location_data;

-- Verify table created and data loaded
DESCRIBE location_data;
SELECT COUNT(*) FROM location_data;
SELECT * FROM location_data LIMIT 5;
```

### 3.5: Create Weather Table
```sql
-- Drop table if exists for fresh start
DROP TABLE IF EXISTS weather_data;

-- Create managed table for weather data  
CREATE TABLE weather_data (
    location_id INT,
    date_recorded STRING,
    weather_code INT,
    temperature_2m_max DOUBLE,
    temperature_2m_min DOUBLE,
    temperature_2m_mean DOUBLE,
    apparent_temperature_max DOUBLE,
    apparent_temperature_min DOUBLE,
    apparent_temperature_mean DOUBLE,
    daylight_duration DOUBLE,
    sunshine_duration DOUBLE,
    precipitation_sum DOUBLE,
    rain_sum DOUBLE,
    precipitation_hours DOUBLE,
    wind_speed_10m_max DOUBLE,
    wind_gusts_10m_max DOUBLE,
    wind_direction_10m_dominant INT,
    shortwave_radiation_sum DOUBLE,
    et0_fao_evapotranspiration DOUBLE,
    sunrise STRING,
    sunset STRING
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
STORED AS TEXTFILE
TBLPROPERTIES ('skip.header.line.count'='1');

-- Load data into the table
LOAD DATA INPATH '/user/hive/warehouse/weather_analytics.db/weather/weatherData.csv' INTO TABLE weather_data;

-- Verify table created and data loaded
DESCRIBE weather_data;
SELECT COUNT(*) FROM weather_data;
SELECT * FROM weather_data LIMIT 5;
```

### 3.6: Verify both tables
```sql
-- Show all tables
SHOW TABLES;

-- Check record counts
SELECT COUNT(*) as location_count FROM location_data;
SELECT COUNT(*) as weather_count FROM weather_data;

-- Preview joined data
SELECT l.city_name, w.date_recorded, w.temperature_2m_max, w.et0_fao_evapotranspiration
FROM weather_data w
JOIN location_data l ON w.location_id = l.location_id
LIMIT 10;
```

---

## STEP 4: Task 1 - Top 10 Most Temperate Cities

### 4.1: Understanding the query
```
Goal: Find cities with most stable temperatures (lowest variation)
Method: Calculate standard deviation of temperature_2m_max
Result: Cities with lowest std dev = most temperate
```

### 4.2: Run the query
```sql
-- Calculate temperature stability metrics for each city
-- Lower standard deviation = more temperate (stable) climate
SELECT 
    l.city_name AS city,
    COUNT(*) AS total_observations,
    ROUND(AVG(w.temperature_2m_max), 2) AS avg_max_temp,
    ROUND(MIN(w.temperature_2m_max), 2) AS min_max_temp,
    ROUND(MAX(w.temperature_2m_max), 2) AS max_max_temp,
    ROUND(STDDEV(w.temperature_2m_max), 2) AS temp_std_deviation,
    ROUND(MAX(w.temperature_2m_max) - MIN(w.temperature_2m_max), 2) AS temp_range
FROM 
    weather_data w
JOIN 
    location_data l ON w.location_id = l.location_id
WHERE 
    w.temperature_2m_max IS NOT NULL
GROUP BY 
    l.city_name
ORDER BY 
    temp_std_deviation ASC,
    temp_range ASC
LIMIT 10;
```

### 4.3: Save results to table
```sql
-- Drop table if exists
DROP TABLE IF EXISTS top_temperate_cities;

-- Create table with results
CREATE TABLE top_temperate_cities AS
SELECT 
    l.city_name AS city,
    COUNT(*) AS total_observations,
    ROUND(AVG(w.temperature_2m_max), 2) AS avg_max_temp,
    ROUND(MIN(w.temperature_2m_max), 2) AS min_max_temp,
    ROUND(MAX(w.temperature_2m_max), 2) AS max_max_temp,
    ROUND(STDDEV(w.temperature_2m_max), 2) AS temp_std_deviation,
    ROUND(MAX(w.temperature_2m_max) - MIN(w.temperature_2m_max), 2) AS temp_range
FROM 
    weather_data w
JOIN 
    location_data l ON w.location_id = l.location_id
WHERE 
    w.temperature_2m_max IS NOT NULL
GROUP BY 
    l.city_name
ORDER BY 
    temp_std_deviation ASC,
    temp_range ASC
LIMIT 10;

-- Display the saved results
SELECT * FROM top_temperate_cities;
```

---

## STEP 5: Task 2 - Seasonal Evapotranspiration Analysis

### 5.1: Understanding the query
```
Goal: Calculate average ET for two agricultural seasons by district
Seasons:
  - Maha Season: September to March (months 9,10,11,12,1,2,3)
  - Yala Season: April to August (months 4,5,6,7,8)
Method: Parse date, classify by season, aggregate by district
```

### 5.2: Create temporary table with parsed dates
```sql
-- Drop table if exists
DROP TABLE IF EXISTS weather_with_month;

-- Create table with month extracted from date
CREATE TABLE weather_with_month AS
SELECT 
    location_id,
    date_recorded,
    et0_fao_evapotranspiration,
    CAST(SPLIT(date_recorded, '/')[0] AS INT) AS month_num
FROM 
    weather_data
WHERE 
    et0_fao_evapotranspiration IS NOT NULL;

-- Verify temp table
SELECT * FROM weather_with_month LIMIT 10;
SELECT COUNT(*) FROM weather_with_month;
```

### 5.3: Calculate seasonal averages
```sql
-- Calculate seasonal evapotranspiration by district/city
SELECT 
    l.city_name AS district,
    CASE 
        WHEN w.month_num IN (9, 10, 11, 12, 1, 2, 3) THEN 'Maha_Season_Sep_to_Mar'
        WHEN w.month_num IN (4, 5, 6, 7, 8) THEN 'Yala_Season_Apr_to_Aug'
    END AS agricultural_season,
    ROUND(AVG(w.et0_fao_evapotranspiration), 2) AS avg_evapotranspiration_mm,
    COUNT(*) AS total_observations,
    ROUND(MIN(w.et0_fao_evapotranspiration), 2) AS min_evapotranspiration,
    ROUND(MAX(w.et0_fao_evapotranspiration), 2) AS max_evapotranspiration
FROM 
    weather_with_month w
JOIN 
    location_data l ON w.location_id = l.location_id
GROUP BY 
    l.city_name,
    CASE 
        WHEN w.month_num IN (9, 10, 11, 12, 1, 2, 3) THEN 'Maha_Season_Sep_to_Mar'
        WHEN w.month_num IN (4, 5, 6, 7, 8) THEN 'Yala_Season_Apr_to_Aug'
    END
ORDER BY 
    l.city_name,
    agricultural_season;
```

### 5.4: Save overall seasonal results
```sql
-- Drop table if exists
DROP TABLE IF EXISTS seasonal_evapotranspiration;

-- Create table with overall seasonal averages
CREATE TABLE seasonal_evapotranspiration AS
SELECT 
    l.city_name AS district,
    CASE 
        WHEN w.month_num IN (9, 10, 11, 12, 1, 2, 3) THEN 'Maha_Season_Sep_to_Mar'
        WHEN w.month_num IN (4, 5, 6, 7, 8) THEN 'Yala_Season_Apr_to_Aug'
    END AS agricultural_season,
    ROUND(AVG(w.et0_fao_evapotranspiration), 2) AS avg_evapotranspiration_mm,
    COUNT(*) AS total_observations,
    ROUND(MIN(w.et0_fao_evapotranspiration), 2) AS min_evapotranspiration,
    ROUND(MAX(w.et0_fao_evapotranspiration), 2) AS max_evapotranspiration,
    ROUND(STDDEV(w.et0_fao_evapotranspiration), 2) AS std_evapotranspiration
FROM 
    weather_with_month w
JOIN 
    location_data l ON w.location_id = l.location_id
GROUP BY 
    l.city_name,
    CASE 
        WHEN w.month_num IN (9, 10, 11, 12, 1, 2, 3) THEN 'Maha_Season_Sep_to_Mar'
        WHEN w.month_num IN (4, 5, 6, 7, 8) THEN 'Yala_Season_Apr_to_Aug'
    END;

-- Display the saved results
SELECT * FROM seasonal_evapotranspiration ORDER BY district, agricultural_season;
```

---

## STEP 6: Year-by-Year Breakdown (For Detailed Analysis)

### 6.1: Create temp table with year and month
```sql
-- Drop table if exists
DROP TABLE IF EXISTS weather_with_month_year;

-- Extract both month and year
CREATE TABLE weather_with_month_year AS
SELECT 
    location_id,
    date_recorded,
    et0_fao_evapotranspiration,
    CAST(SPLIT(date_recorded, '/')[0] AS INT) AS month_num,
    CAST(SPLIT(date_recorded, '/')[2] AS INT) AS year_num
FROM 
    weather_data
WHERE 
    et0_fao_evapotranspiration IS NOT NULL;

-- Verify
SELECT * FROM weather_with_month_year LIMIT 10;
```

### 6.2: Calculate year-by-year seasonal averages
```sql
-- Drop table if exists
DROP TABLE IF EXISTS seasonal_evapotranspiration_by_year;

-- Create year-by-year breakdown
CREATE TABLE seasonal_evapotranspiration_by_year AS
SELECT 
    l.city_name AS district,
    w.year_num AS year,
    CASE 
        WHEN w.month_num IN (9, 10, 11, 12, 1, 2, 3) THEN 'Maha_Season_Sep_to_Mar'
        WHEN w.month_num IN (4, 5, 6, 7, 8) THEN 'Yala_Season_Apr_to_Aug'
    END AS agricultural_season,
    ROUND(AVG(w.et0_fao_evapotranspiration), 2) AS avg_evapotranspiration_mm,
    COUNT(*) AS total_observations
FROM 
    weather_with_month_year w
JOIN 
    location_data l ON w.location_id = l.location_id
GROUP BY 
    l.city_name,
    w.year_num,
    CASE 
        WHEN w.month_num IN (9, 10, 11, 12, 1, 2, 3) THEN 'Maha_Season_Sep_to_Mar'
        WHEN w.month_num IN (4, 5, 6, 7, 8) THEN 'Yala_Season_Apr_to_Aug'
    END
ORDER BY 
    l.city_name,
    w.year_num,
    agricultural_season;

-- Display year-by-year results
SELECT * FROM seasonal_evapotranspiration_by_year;
```

### 6.3: Query specific examples
```sql
-- View specific district year-by-year
SELECT * FROM seasonal_evapotranspiration_by_year 
WHERE district = 'Colombo' 
ORDER BY year, agricultural_season;

-- Compare two districts
SELECT * FROM seasonal_evapotranspiration_by_year 
WHERE district IN ('Colombo', 'Jaffna')
ORDER BY district, year, agricultural_season;

-- Find highest ET years
SELECT district, year, agricultural_season, avg_evapotranspiration_mm 
FROM seasonal_evapotranspiration_by_year 
WHERE agricultural_season = 'Yala_Season_Apr_to_Aug'
ORDER BY avg_evapotranspiration_mm DESC 
LIMIT 10;
```

---

## STEP 7: Export Results to CSV Files

### 7.1: Export Task 1 results
```sql
-- Export top temperate cities (WITHOUT headers)
INSERT OVERWRITE LOCAL DIRECTORY '/tmp/task1_results'
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
SELECT * FROM top_temperate_cities;
```

**Note:** This export does NOT include column headers. Headers are added in Step 8.

### 7.2: Export Task 2 overall results
```sql
-- Export overall seasonal averages (WITHOUT headers)
INSERT OVERWRITE LOCAL DIRECTORY '/tmp/task2_overall_results'
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
SELECT * FROM seasonal_evapotranspiration 
ORDER BY district, agricultural_season;
```

### 7.3: Export Task 2 year-by-year results
```sql
-- Export year-by-year breakdown (WITHOUT headers)
INSERT OVERWRITE LOCAL DIRECTORY '/tmp/task2_yearly_results'
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
SELECT * FROM seasonal_evapotranspiration_by_year 
ORDER BY district, year, agricultural_season;
```

### 7.4: Alternative - Export with Headers Using SELECT
If you want headers, use this method instead:

```sql
-- Task 1 with headers
INSERT OVERWRITE LOCAL DIRECTORY '/tmp/task1_with_headers'
ROW FORMAT DELIMITED
FIELDS TERMINATED BY ','
SELECT 'city' as city, 
       'total_observations' as total_observations,
       'avg_max_temp' as avg_max_temp,
       'min_max_temp' as min_max_temp,
       'max_max_temp' as max_max_temp,
       'temp_std_deviation' as temp_std_deviation,
       'temp_range' as temp_range
UNION ALL
SELECT city, 
       CAST(total_observations AS STRING),
       CAST(avg_max_temp AS STRING),
       CAST(min_max_temp AS STRING),
       CAST(max_max_temp AS STRING),
       CAST(temp_std_deviation AS STRING),
       CAST(temp_range AS STRING)
FROM top_temperate_cities
ORDER BY temp_std_deviation ASC;
```

**Simpler approach:** Add headers after export (shown in Step 8)

### 7.4: Exit Hive and container
```sql
-- Exit hive shell
exit;
```

```bash
# Exit hive-server container
exit
```

---

## STEP 8: Copy Results to Local Machine and Add Headers

### 8.1: Copy files from container
```bash
# Copy Task 1 results
sudo docker cp hive-server:/tmp/task1_results/000000_0 ./task1_top_temperate_cities.csv

# Copy Task 2 overall results
sudo docker cp hive-server:/tmp/task2_overall_results/000000_0 ./task2_seasonal_evapotranspiration.csv

# Copy Task 2 year-by-year results
sudo docker cp hive-server:/tmp/task2_yearly_results/000000_0 ./task2_seasonal_evapotranspiration_by_year.csv

# Verify files created
ls -lh *.csv
```

### 8.2: Add column headers to CSV files

**Option A: Add headers inline (Quick)**
```bash
# Task 1 - Add header
sed -i '1i\city,total_observations,avg_max_temp,min_max_temp,max_max_temp,temp_std_deviation,temp_range' task1_top_temperate_cities.csv

# Task 2 Overall - Add header
sed -i '1i\district,agricultural_season,avg_evapotranspiration_mm,total_observations,min_evapotranspiration,max_evapotranspiration,std_evapotranspiration' task2_seasonal_evapotranspiration.csv

# Task 2 Year-by-Year - Add header
sed -i '1i\district,year,agricultural_season,avg_evapotranspiration_mm,total_observations' task2_seasonal_evapotranspiration_by_year.csv
```

**Option B: Create new files with headers (Safer)**
```bash
# Task 1
echo "city,total_observations,avg_max_temp,min_max_temp,max_max_temp,temp_std_deviation,temp_range" > task1_with_headers.csv
cat task1_top_temperate_cities.csv >> task1_with_headers.csv

# Task 2 Overall
echo "district,agricultural_season,avg_evapotranspiration_mm,total_observations,min_evapotranspiration,max_evapotranspiration,std_evapotranspiration" > task2_overall_with_headers.csv
cat task2_seasonal_evapotranspiration.csv >> task2_overall_with_headers.csv

# Task 2 Year-by-Year
echo "district,year,agricultural_season,avg_evapotranspiration_mm,total_observations" > task2_yearly_with_headers.csv
cat task2_seasonal_evapotranspiration_by_year.csv >> task2_yearly_with_headers.csv
```

### 8.3: Verify results
```bash
# View first few lines with headers
head -5 task1_top_temperate_cities.csv
head -5 task2_seasonal_evapotranspiration.csv
head -5 task2_seasonal_evapotranspiration_by_year.csv
```

---

## STEP 9: Verify Your Work

### 9.1: Check file contents
```bash
# Count rows in each file
echo "Task 1 - Top 10 cities:"
wc -l task1_top_temperate_cities.csv

echo "Task 2 - Overall averages (27 districts × 2 seasons = 54 rows):"
wc -l task2_seasonal_evapotranspiration.csv

echo "Task 2 - Year-by-year (27 districts × 15 years × 2 seasons = 810 rows):"
wc -l task2_seasonal_evapotranspiration_by_year.csv
```

### 9.2: Re-enter Hive to verify tables exist
```bash
sudo docker exec -it hive-server bash
hive
```

```sql
USE weather_analytics;
SHOW TABLES;

-- Should see:
-- location_data
-- weather_data
-- top_temperate_cities
-- seasonal_evapotranspiration
-- seasonal_evapotranspiration_by_year
-- weather_with_month
-- weather_with_month_year

-- Quick verification queries
SELECT COUNT(*) FROM top_temperate_cities;  -- Should be 10
SELECT COUNT(*) FROM seasonal_evapotranspiration;  -- Should be 54
SELECT COUNT(*) FROM seasonal_evapotranspiration_by_year;  -- Should be 810

exit;
```

---

## Summary of What You Created

✅ **Database:** `weather_analytics`

✅ **Tables:**
1. `location_data` - 28 cities
2. `weather_data` - ~142,000 observations
3. `top_temperate_cities` - Top 10 cities by temperature stability
4. `seasonal_evapotranspiration` - Overall seasonal averages (54 rows)
5. `seasonal_evapotranspiration_by_year` - Year-by-year breakdown (810 rows)

✅ **CSV Files:**
1. `task1_top_temperate_cities.csv` - 10 rows
2. `task2_seasonal_evapotranspiration.csv` - 54 rows
3. `task2_seasonal_evapotranspiration_by_year.csv` - 810 rows

---

## Key Points for Viva

### Task 1 - Top 10 Temperate Cities
- **Method:** Standard deviation of temperature_2m_max
- **Winner:** Matara (1.08°C std dev)
- **Why:** Low variation = stable climate = good for agriculture

### Task 2 - Seasonal Evapotranspiration
- **Two seasons:** Maha (Sep-Mar) and Yala (Apr-Aug)
- **Purpose:** Calculate water requirements for irrigation
- **Finding:** Yala season generally has higher ET (more water needed)
- **Coverage:** 15 years (2010-2024), 27 districts

### Technical Concepts
- **Managed vs External tables:** Used managed tables with LOAD DATA
- **Date parsing:** Used SPLIT() function to extract month/year
- **Aggregation:** GROUP BY with CASE statements for season classification
- **Joins:** Combined location and weather data
- **Statistical functions:** AVG, MIN, MAX, STDDEV, COUNT

---

## Troubleshooting Commands

```bash
# If you need to start over
sudo docker exec -it hive-server bash -c "hive -e 'DROP DATABASE IF EXISTS weather_analytics CASCADE;'"

# Check HDFS data
sudo docker exec -it namenode bash
hdfs dfs -ls /user/hive/warehouse/weather_analytics.db/
exit

# Check container logs
sudo docker logs hive-server
sudo docker logs namenode

# Restart container if needed
sudo docker restart hive-server
```

---

**Good luck with your viva! 🎓**

You now understand every step of the process and can explain:
- Why you chose these queries
- How the data flows from HDFS to Hive to results
- What the agricultural implications are
- How the year-by-year analysis provides deeper insights
