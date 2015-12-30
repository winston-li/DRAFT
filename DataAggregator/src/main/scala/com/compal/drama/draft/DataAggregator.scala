package com.compal.drama.draft



import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.{Logging, SparkConf, SparkContext}
import org.apache.spark.rdd.{RDD, PairRDDFunctions}
import org.apache.spark.util.StatCounter
import org.apache.spark.SparkContext._

import com.datastax.spark.connector._
import com.datastax.spark.connector.rdd.CassandraRDD
import org.joda.time.DateTime
import scala.collection.mutable

private[draft] final class DASettings(conf: Option[Config] = None) {
  val cfg = conf match {
    case Some(c) => c.withFallback(ConfigFactory.load)
    case _ => ConfigFactory.load()
  }

  val AppName = cfg.getString("dataaggregator.app-name")

  protected val cassandra = cfg.getConfig("dataaggregator.cassandra")
  val CassandraHosts = cassandra.getString("connection.hosts")
  val CassandraKeyspace = cassandra.getString("keyspace")
  val CassandraTableRaw = cassandra.getString("table.raw")
  val CassandraTableAggregTemperature = cassandra.getString("table.aggreg-temperature")
}

object DataAggregator extends App with Logging {
  val settings = new DASettings
  import DataModel._
  import settings._

  val (year, month, master): (Int, Int, String) = args.toSeq match {
    case arg1 +: arg2 +: arg3 +: others => (arg1.toInt, arg2.toInt, arg3)
    case arg1 +: arg2 +: other => (arg1.toInt, arg2.toInt, "local[*]")
    case _ => throw new IllegalArgumentException("Must provide the \"year\" and \"month\" to aggregate")
  }

  val sparkConf = new SparkConf().setAppName(AppName)
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .set("spark.kryo.registrationRequired", "true")
    .set("spark.cassandra.connection.host", CassandraHosts)
    .setMaster(master)
    .registerKryoClasses(Array(classOf[RawWeatherData], classOf[Array[String]], classOf[Array[Array[String]]],
    classOf[mutable.WrappedArray.ofRef[_]], classOf[Array[scala.Tuple2[Any, Any]]]))

  val sc = new SparkContext(sparkConf)
  val startTime = System.currentTimeMillis

  val resultSet = sc.cassandraTable[(String, Double)](CassandraKeyspace, CassandraTableRaw)
                  .select("wsid", "temperature")
                  .where("year = ? AND month = ?", year, month)

  resultSet.groupByKey().mapValues(StatCounter(_))
    .map { case (k, v) => toMonthlyTemperature(k, year, month, v)}
    .saveToCassandra(CassandraKeyspace, CassandraTableAggregTemperature)

  val elapsedTime = (System.currentTimeMillis - startTime) / 1000  // in seconds
  log.info(s"elapsed time = ${elapsedTime} seconds")
  println(s"elapsed time = ${elapsedTime} seconds")

  private def toMonthlyTemperature(wsid: String, year: Int, month: Int, stats: StatCounter): MonthlyTemperatureStats = {
        MonthlyTemperatureStats(wsid, year, month, high = stats.max, low = stats.min, mean = stats.mean,
          variance = stats.variance, stdev = stats.stdev, DateTime.now())
  }

}
