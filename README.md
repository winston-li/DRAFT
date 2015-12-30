# DRAFT
Distributed Resilient Analytic Framework Type

---
There are three parts:  
##### 1. DataFeeder
A Kafka Producer app feeds time-series sampling data of weather stations to Kafka cluster. It keeps the time-series ordering for each weather station.

##### 2. StreamAnalyzer
A Spark Streaming app performs

        (1) getting time-series data from Kafka cluster
        (2) writing raw data to Cassandra cluster
        (3) calcuating moving average data to Cassandra cluster
        (4) putting an alert to Kafka cluster if sensing an outlier data (based on moving average data)

##### 3. DataAggregator
A on-demand Spark app performs batch processing jobs (calculating monthly statistics) on raw data stored in Cassandra cluster.

