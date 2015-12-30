import sbt._

object Version {
  val jdk = "1.7"
  val scala = "2.10.4"
  val kafka = "0.8.2.0"
  val config = "1.2.1"
  val logging = "1.1.0"
  val spark = "1.3.0"
  val cassandra = "2.0.14"
  val sparkcassandra = "1.3.0-M1"
  val joda = "2.4"
  val jodaconvert = "1.7"
  val commonspool = "2.3"
}

object Library {
  val kafka = "org.apache.kafka" %% "kafka" % Version.kafka
  val config = "com.typesafe" % "config" % Version.config
  val logging = "com.typesafe" %% "scalalogging-slf4j" % Version.logging // EOL, use for Scala 2.10 only
  //val logging = "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0" // use for Scala 2.11
  val spark_kafka = "org.apache.spark" %% "spark-streaming-kafka" % Version.spark
  val spark_core = "org.apache.spark" %% "spark-core" % Version.spark % "provided"
  val spark_streaming = "org.apache.spark" %% "spark-streaming" % Version.spark % "provided"
  val spark_cassandra = "com.datastax.spark" %% "spark-cassandra-connector" % Version.sparkcassandra
  val cassandra = "com.datastax.cassandra" % "cassandra-driver-core" % Version.cassandra
  val joda = "joda-time" % "joda-time" % Version.joda
  val joda_convert = "org.joda" % "joda-convert" % Version.jodaconvert
  val commons_pool = "org.apache.commons" % "commons-pool2" % Version.commonspool
}

object Dependencies {
  import Library._

  val common = Seq (
    kafka,
    config,
    joda,
    joda_convert,
    commons_pool
  )

  val datafeeder = Seq (
    kafka,
    config,
    logging
  )

  val streamanalyzer = Seq (
    config,
    spark_kafka,
    spark_core,
    spark_streaming,
    spark_cassandra,
    cassandra
  )

  val dataaggregrator = Seq (
    config,
    spark_core,
    spark_cassandra,
    cassandra
  )
}
