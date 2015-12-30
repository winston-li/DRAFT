package com.compal.drama.draft

/**
 * Created by Winston on 2015/3/27.
 *
 * (1) Basic code with
 *    -- Scala Config, Scala Log, Scala Resource Management, Scala File Source in IDE & JAR
 * (2) Specialized code for
 *    -- Kafka Producer
 */

import java.io.{File => JFile}
import java.util.Properties
import java.util.jar.JarFile

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.slf4j.Logging
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}

import scala.collection.mutable
import scala.io.Source
import scala.util.control.NonFatal


private[draft] final class DFSettings() {
  protected val cfg =  ConfigFactory.load()

  val kafkaCfg = if (cfg.hasPath("datafeeder.kafka"))
                   Some(cfg.getConfig("datafeeder.kafka"))
                 else
                   None

  protected val data = cfg.getConfig("datafeeder.data")
  val DataPath = data.getString("file.path")
  val DataPeriod = data.getInt("period")
  val DataId = data.getInt("id.ordinal")

  private[this] def getFileSources(path: String): Set[Source] = {
    try {
      val fileSet = new JFile(path).list.toSet
      fileSet.filter(!_.startsWith(".")).map {
        case f if path.endsWith("/") => new JFile(path + f)
        case f => new JFile(path + "/" + f)
      }.map(Source.fromFile(_, "utf-8"))
    }
    catch {
      case NonFatal(ex) =>
        println(s"Cannot list files for URL: $path")
        Set.empty
    }
  }

  private[this] def getResourceSources(path: String = DataPath): Set[Source] = {
    val dirURL = getClass.getClassLoader.getResource(path)
    if (dirURL == null) {
      println(s"Cannot list files for URL: $path")
      return Set.empty
    }

    dirURL.getProtocol match {
      case "file" => // run in IDE
        val fileSet = new JFile(dirURL.toURI).list.map(path + "/" + _).toSet
        fileSet.map(getClass.getClassLoader.getResourceAsStream(_)).map(Source.fromInputStream(_))
      case "jar" => // run in jar
        val jarPath = dirURL.getPath.substring(5, dirURL.getPath.indexOf("!")) // strip out only the JAR file
        val entries = new JarFile(jarPath).entries
        val result = new mutable.HashSet[String].empty
        while (entries.hasMoreElements) {
          val name = entries.nextElement.getName
          if (name.startsWith(path))
            result.add(name)
        }
        val fileSet = result.toSet
        fileSet.map(getClass.getClassLoader.getResourceAsStream(_)).map(Source.fromInputStream(_))
    }
  }

  private[draft] def fileFeed(folderpath: Option[String]): Set[Source] = {
    if (folderpath == None)
      getResourceSources()
    else
      getFileSources(folderpath.get)
  }
}

// use scala idiom (by-name parameter) to automatic manage resource lifecycle
object manage {
  def apply[R <: { def close(): Unit }, T](resource: => R)(f: R => T) = {
    var res: Option[R] = None
    try {
      res = Some(resource)  // Only reference "resource" once here!
      f(res.get)
    }
    catch {
      case NonFatal(ex) => println(s"Non fatal exception! $ex")
    }
    finally {
      if (res != None)
        res.get.close
    }
  }
}

object DataFeeder extends App with Logging {
  // use system properties -Dconfig.file=<filesystem path> (e.g. /Users/Winston/draft/application.conf)
  // to overwrite default  application.conf
  val settings = new DFSettings
  import settings._
  
  val (topic, keyid, datafolder): (Option[String], Option[String], Option[String]) = args.toSeq match {
    case arg1 +: arg2 +: arg3 +: others => (Some(arg1), Some(arg2), Some(arg3))
    case arg1 +: arg2 +: other => (Some(arg1), Some(arg2), None)
    case arg1 +: other => (Some(arg1), None, None)
    case _ => (None, None, None)
  }

  val producer = KafkaProducer(settings.kafkaCfg)

  // read sample files
  val startTime = System.currentTimeMillis
  val eventsSet : Set[scala.io.Source] = fileFeed(datafolder)
  var totalEvents = 0
  eventsSet.foreach {
    manage(_) { source =>
      val events = source.getLines.toSeq
      for (event <- events) {
        val key = if (keyid != None)
                    keyid.get
                  else
                    event.split(',')(DataId) //use wsid as default key

        producer.send(key, event, topic)
        logger.debug(s"data point: $key => $event")
        totalEvents += 1
        Thread.sleep(DataPeriod)
      }
    }
  }
  producer.shutdown()

  val elapsedTime = (System.currentTimeMillis - startTime) / 1000  // in seconds
  logger.info(s"total ${totalEvents} events in ${elapsedTime} seconds => RPS = ${totalEvents / elapsedTime}")
}
