package com.compal.drama.draft

/**
 * Created by Winston on 2015/6/7.
 */

import java.util.Properties
import com.typesafe.config.{Config, ConfigFactory}
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}

import org.apache.commons.pool2.impl.DefaultPooledObject
import org.apache.commons.pool2.{PooledObject, BasePooledObjectFactory}

private[draft] final class KFKSettings(conf: Option[Config] = None) {
  protected val cfg = conf match {
    case Some(c) => c.withFallback(ConfigFactory.load.getConfig("common.kafka"))
    case _ => ConfigFactory.load.getConfig("common.kafka")
  }

  val KafkaTopic = cfg.getString("put.topic")
  val KafkaBrokers = cfg.getString("brokers")
  val KafkaEncoder = cfg.getString("encoder.fqcn")
  val KafkaPartitioner = cfg.getString("partitioner.fqcn")
  val KafkaAcks = cfg.getString("acks")
}

case class KafkaProducer(conf: Option[Config] = None) {

  val settings = new KFKSettings(conf)
  import settings._

  type Key = String
  type Val = String

  private val props = new Properties()
  props.put("metadata.broker.list", KafkaBrokers)
  props.put("serializer.class", KafkaEncoder)
  props.put("partitioner.class", KafkaPartitioner)
  props.put("request.required.acks", KafkaAcks)

  private val config = new ProducerConfig(props)
  private val producer = new Producer[Key, Val](config)

  private def toMessage(value: Val, key: Option[Key] = None, topic: Option[String] = None): KeyedMessage[Key, Val] = {
    val t = topic.getOrElse(KafkaTopic)
    key match {
      case Some(k) => new KeyedMessage(t, k, value)
      case _ => new KeyedMessage(t, value)
    }
  }

  def send(key: Key, value: Val, topic: Option[String] = None) {
    producer.send(toMessage(value, Option(key), topic))
  }

  def send(value: Val, topic: Option[String]) {
    send(null, value, topic)
  }

  def send(value: Val, topic: String) {
    send(null, value, Option(topic))
  }

  def send(value: Val) {
    send(null, value, None)
  }

  def shutdown(): Unit = producer.close()

}


class KafkaProducerFactory(conf: Option[Config] = None) extends Serializable {
  def newInstance() = new KafkaProducer(conf)
}

class PooledKafkaProducerFactory(val factory: KafkaProducerFactory) extends BasePooledObjectFactory[KafkaProducer] with Serializable {

  override def create(): KafkaProducer = factory.newInstance()

  override def wrap(obj: KafkaProducer): PooledObject[KafkaProducer] = new DefaultPooledObject(obj)

  // From the Commons Pool docs: "Invoked on every instance when it is being "dropped" from the pool.  There is no
  // guarantee that the instance being destroyed will be considered active, passive or in a generally consistent state."
  override def destroyObject(p: PooledObject[KafkaProducer]): Unit = {
    p.getObject.shutdown()
    super.destroyObject(p)
  }

}