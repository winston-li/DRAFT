package com.compal.drama.draft

/**
 * Created by Winston on 2015/4/6.
 *
 * (1) Basic code with
 *    -- Scala Config, Spark Log, Spark Config
 * (2) Specialized code for SparkStreaming
 *    -- Driver HA, Shutdown, Sliding Window Compute
 *    -- Kafka Direct Stream, Cassandra Connector
 */


import com.typesafe.config.{Config, ConfigFactory}
import kafka.serializer.StringDecoder
//import org.apache.commons.pool2.impl.{BaseGenericObjectPool, GenericObjectPool, GenericObjectPoolConfig}
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Milliseconds, StreamingContext}
import org.apache.spark.{Logging, SparkConf}

import com.datastax.spark.connector.streaming._

import scala.collection.mutable


private[draft] final class SASettings(conf: Option[Config] = None) {
  val cfg = conf match {
    case Some(c) => c.withFallback(ConfigFactory.load)
    case _ => ConfigFactory.load()
  }

  val AppName = cfg.getString("streamanalyzer.app-name")

  protected val spark = cfg.getConfig("streamanalyzer.spark")
  val SparkIngestRateMax = spark.getString("mbatch.ingest_rate.max")
  val SparkStreamingDuration = spark.getInt("mbatch.duration")
  val SparkStreamingWindow = spark.getInt("mbatch.window") * SparkStreamingDuration
  val SparkStreamingSlide = spark.getInt("mbatch.slide") * SparkStreamingDuration
  val SparkCheckpointDir = spark.getString("checkpoint.dir")
  val SparkCheckpointInterval = spark.getInt("checkpoint.interval") * SparkStreamingDuration

  protected val cassandra = cfg.getConfig("streamanalyzer.cassandra")
  val CassandraHosts = cassandra.getString("connection.hosts")
  val CassandraConnKeepAliveTime = (cassandra.getInt("connection.keepalive") + SparkStreamingDuration).toString
  val CassandraKeyspace = cassandra.getString("keyspace")
  val CassandraTableRaw = cassandra.getString("table.raw")
  val CassandraTableMAvgTemperature = cassandra.getString("table.moving-avg-temperature")

  val kafka = cfg.getConfig("streamanalyzer.kafka")
  val KafkaTopics = kafka.getString("get.topics")
  val KafkaConsumerGroup= kafka.getString("group.id")
  val KafkaBrokers = kafka.getString("brokers")

  protected val dataformat = cfg.getConfig("streamanalyzer.data-format")
  val DFId = dataformat.getInt("id.ordinal")
  val DFTemperature = dataformat.getInt("temperature.ordinal")
  val DFTemperatureMetricThreshold = dataformat.getDouble("temperature.metric.threshold")
}


object StreamAnalyzer extends App with Logging {
  // use system properties -Dconfig.file=<filesystem path> (e.g. /Users/Winston/draft/application.conf)
  // to overwrite default  application.conf
  val settings = new SASettings
  import DataModel._
  import settings._

  val master: String = args.length match {
    case x: Int if x == 1 => args(0)
    case _ => "local[*]"
  }
  
/*
  private def createKafkaProducerPool(conf: Option[Config]): GenericObjectPool[KafkaProducer] = {
    val producerFactory = new KafkaProducerFactory(conf)
    val pooledProducerFactory = new PooledKafkaProducerFactory(producerFactory)
    val poolConfig = {
      val c = new GenericObjectPoolConfig
      val maxNumProducers = 10
      c.setMaxTotal(maxNumProducers)
      c.setMaxIdle(maxNumProducers)
      c
    }
    new GenericObjectPool[KafkaProducer](pooledProducerFactory, poolConfig)
  }
*/

  def createStreamingContext(): StreamingContext = {
    val sparkConf = new SparkConf().setAppName(AppName)
      .set("spark.streaming.kafka.maxRatePerPartition", SparkIngestRateMax)
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.registrationRequired", "true")
      .set("spark.cassandra.connection.host", CassandraHosts)
      .set("spark.cassandra.connection.keep_alive_ms", CassandraConnKeepAliveTime)
      .setMaster(master)
      .registerKryoClasses(Array(classOf[RawWeatherData], classOf[Array[String]], classOf[Array[Array[String]]],
                           classOf[mutable.WrappedArray.ofRef[_]], classOf[Array[scala.Tuple2[Any, Any]]]))
      /* add classes per runtime exception
         When encounter a NotSerializableException, get detailed serialization trace via
         bin/spark-submit --driver-java-options "-Dsun.io.serialization.extendedDebugInfo=true" ....
      */

    val ssc =  new StreamingContext(sparkConf, Milliseconds(SparkStreamingDuration))
    processKafkaStream(ssc)

    ssc.checkpoint(SparkCheckpointDir) // TODO: validate checkpoint for driver & stateful streaming resilience
    ssc
  }
  //val ssc = StreamingContext.getOrCreate(SparkCheckpointDir, createStreamingContext _) // HA version
  val ssc = createStreamingContext // Non-HA version

  def processKafkaStream(ssc: StreamingContext) = {
    // Create direct kafka stream with brokers and topics
    // In Spark 1.3.0, createDirectStream for Kafka integration uses kafka brokers, not zookeeper
    val topicsSet = KafkaTopics.split(",").toSet
    val kafkaParams = Map[String, String](
      "metadata.broker.list" -> KafkaBrokers,
      "group.id" -> KafkaConsumerGroup,
      "auto.offset.reset" -> "smallest"
    )
    log.info(s"Connecting to topics ${topicsSet} via metadata-brokers ${KafkaBrokers}")
    val numInputMessages = ssc.sparkContext.accumulator(0L, "Kafka messages consumed") //TODO: validate this later on
    println(s"Accumulator numInputMessages = ${numInputMessages.value}")
    val kafkaStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)
    kafkaStream.checkpoint(Milliseconds(SparkCheckpointInterval))

    kafkaStream.cache() //TODO: validate cache() for reusing dStream (raw & simple statistics)

    // Write raw streaming data to Cassandra table
    val rawdataStream = kafkaStream.map(_._2.split(","))
    rawdataStream.map(RawWeatherData(_)).saveToCassandra(CassandraKeyspace, CassandraTableRaw)

    val temperatureStream = kafkaStream.map { case (key, value) =>
      (key, value.split(',')(DFTemperature).toDouble)
    }

    val temperatureAggregStream = kafkaStream.map { case (key, value) =>
      (key, (value.split(',')(DFTemperature).toDouble, 1))
    }

    // give up apache common pool due to it can't be serialized....
    /*
    val kafkaProducerPool = {
      val pool = createKafkaProducerPool(Option(settings.kafka))
      ssc.sparkContext.broadcast(pool)
    }
    */

    val aggregStream = temperatureAggregStream.reduceByKeyAndWindow(reduce, invReduce, Milliseconds(SparkStreamingWindow), Milliseconds(SparkStreamingSlide), 2)
    val mavgStream = aggregStream.filter { case (_, (_, count)) => count > 0 }.mapValues { case (sum, count) => sum / count }
    // Write moving average data to Cassandra table
    mavgStream.map { case (k, v) => MovingAvgTemperatureData(k, v) }.saveToCassandra(CassandraKeyspace, CassandraTableMAvgTemperature)

    val joinStream = mavgStream.join(temperatureStream)
    joinStream.foreachRDD { rdd =>
      rdd.foreachPartition { partitionOfRecords =>
        //val p = kafkaProducerPool.value.borrowObject()
        val p = KafkaProducer(Option(settings.kafka))

        partitionOfRecords.filter { case (key, (avg, value)) =>
          if (value > avg * (1.0 + DFTemperatureMetricThreshold) || value < avg * (1.0 - DFTemperatureMetricThreshold))
            true
          else
            false
        }.foreach { case (key, (avg, value)) =>
          p.send(s"ALARM!! for key($key): temperature moving avg is $avg, temperature is $value")
          println(s"********ALARM!! key($key): temperature moving avg is $avg, temperature is $value")
        }

        p.shutdown()
        //kafkaProducerPool.value.returnObject(p)
      }
    }

// blocks below for debug purpose only
/*
    joinStream.foreachRDD { rdd =>
      rdd.foreachPartition { partitionOfRecords =>
        partitionOfRecords.foreach { case (key, (avg, value)) =>
          println(s"$key avg = $avg ($value)")
        }
      }
    }

    rawdataStream.foreachRDD { rdd =>
      rdd.mapPartitionsWithIndex { (i, iter) =>
        println(s"partition# ${i} w/ ${iter.length} rdds")
        Iterator.empty
      }.foreach {
        (_: Nothing) => ()
      }
    }
*/

  }

  def reduce(reduced: (Double, Int), pair: (Double, Int)) = {
    (reduced._1 + pair._1, reduced._2 + pair._2)
  }

  def invReduce(reduced: (Double, Int), pair: (Double, Int)) = {
    (reduced._1 - pair._1, reduced._2 - pair._2)
  }

  // Setup for graceful shutdown of spark streaming
  sys.ShutdownHookThread {
    log.info("Gracefully stopping Spark Streaming Application StreamAnalyzer")
    ssc.stop(true, true)
    log.info("Application StreamAnalyzer stopped")
  }

  // Start the computation
  ssc.start()
  ssc.awaitTermination()
}
