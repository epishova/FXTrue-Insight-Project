import org.apache.spark.sql.types._
import org.apache.spark.sql.functions.expr

// Define Kafka brokers:
val broker = "ec2-18-209-75-68.compute-1.amazonaws.com:9092,ec2-18-205-142-57.compute-1.amazonaws.com:9092,ec2-50-17-32-144.compute-1.amazonaws.com:9092"

val dfraw = spark
.readStream
.format("kafka")
.option("kafka.bootstrap.servers", broker)
.option("subscribe", "currency_exchange")
.load()

// Define schema for the input data consumed from Kafka
val schema = StructType(
  Seq(
    StructField("fx_marker", StringType, false),
    StructField("timestamp_ms", StringType, false),
    StructField("bid_big", StringType, false),
    StructField("bid_points", StringType, false),
    StructField("offer_big", StringType, false),
    StructField("offer_points", StringType, false),
    StructField("hight", StringType, false),
    StructField("low", StringType, false),
    StructField("open", StringType, false)
  )
)

val df = dfraw
.selectExpr("CAST(value AS STRING)").as[String]
.flatMap(_.split("\n"))

val jsons = df.select(from_json($"value", schema) as "data").select("data.*")

val parsed = jsons
  .withColumn("bid_big", $"bid_big".cast(DoubleType))
  .withColumn("bid_points", $"bid_points".cast(IntegerType))
  .withColumn("offer_big", $"offer_big".cast(DoubleType))
  .withColumn("offer_points", $"offer_points".cast(IntegerType))
  .withColumn("hight", $"hight".cast(DoubleType))
  .withColumn("low", $"low".cast(DoubleType))
  .withColumn("open", $"open".cast(DoubleType))
  .withColumn("timestamp_dt", to_timestamp(from_unixtime($"timestamp_ms"/1000.0, "yyyy-MM-dd HH:mm:ss.SSS")))
  .drop("_tmp").filter("fx_marker != ''")

// Compute how many FX rates were generated every 10 seconds. 
// If that number goes beyond statistical bounds then raise Anomaly Detection Alert
val countAD = parsed
.filter($"fx_marker" isin ("USD/JPY"))
.select($"fx_marker", $"timestamp_dt", $"bid_points")
.withWatermark("timestamp_dt", "1 minute")
.groupBy(
  window($"timestamp_dt", "10 seconds"),
  $"fx_marker"
).count()
.filter($"count" < 30 || $"count" > 40)

val sinkKafkaADSimpleCount = filterCountAD
.selectExpr("CAST(fx_marker AS STRING) AS key", "to_json(struct(*)) AS value")
.writeStream
.format("kafka")
.option("kafka.bootstrap.servers", broker)
.option("topic", "ad_simple_count")
.option("checkpointLocation", "/home/ubuntu/kafka_sink_chkp/sink_filterCountADSimpleCount")
.outputMode("complete")
.start()

