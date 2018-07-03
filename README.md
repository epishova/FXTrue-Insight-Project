# FXTrue - Insight Project

Real-time analysis and manipulation detection for currency exchange

### Introduction

This project was developed as part of Insight Data Engineering Fellow Program.

The goal was to build a distributed, streaming pipeline for real-time analysis and manipulation detection for currency exchange.

First, I ingest real-time streaming data from FX provider. Then, I analyze and monitor incoming data to identify current FX trends and detect anomalies in the stream (Kafka, Spark, Cassandra, Python, Scala). Finally, I create analytical dashboards to display reactive real-time metrics (Dash)

### Pipeline

This code was run on Amazon AWS servers. 

I get FX rates from [TrueFX Market Data](https://www.truefx.com/?page=frontpage) by using their Web API. The HTTP client is implemented in `http_ce_producer.py`. This script produces data to Kafka topic.

Please note that you need to register at TrueFX Market Data in order to get data. The registration is free. After that provide your credentials in `http_ce_producer.py`.

This Kafka topic is consumed by the Spark cluster and Cassandra. All computations and analysis are made by Spark. Cassandra is used to store FX rates for historical analysis. Additionally, it stores the results of analysis made by Spark cluster.

Dash application takes data from Cassandra and visualizes it. It plots current FX rates, running averages, and monitoring alerts if the incoming data looks malicious.

### Repo Directory Structure

`HTTP` contains a client getting data from FX provider.

`Kafka` contains a script to create topics.

`Cassandra` contains all scripts which insert data into the database. 

`Spark` contains scripts computing trends and detecting data manipulation in the stream.
