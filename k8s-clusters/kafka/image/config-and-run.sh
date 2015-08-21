#!/bin/bash


#Find the broker id 
# => try to leverage etcd's atomically creating in-order keys as broker id if new created pod
# => use /kafka_data/myid for existing pod

ETCD2_ENDPOINT="$(ip route | awk '/\<default via\>/ { print $3}'):2379"
if [ -f /kafka_data/data/myid ]; then
  BROKER_ID=`(cat /kafka_data/data/myid)`
  echo "use existing broker id: ${BROKER_ID}"
else
  BROKER_ID=`(curl -L http://${ETCD2_ENDPOINT}/v2/keys/${POD_NAMESPACE}/kafka -XPOST | sed 's/.*createdIndex":\([0-9]*\).*/\1/')`
  echo ${BROKER_ID} > /kafka_data/data/myid
  echo "create a new broker id: ${BROKER_ID}"
fi

sed -i "s/%%BROKER_ID%%/`echo ${BROKER_ID}`/g;s/%%ZOOKEEPER_CONNECT%%/`echo ${ZOOKEEPER_CONNECT}`/g" /opt/kafka/config/server.properties

#export CLASSPATH=$CLASSPATH:/opt/kafka/lib/slf4j-log4j12.jar
#export JMX_PORT=7203

cat /opt/kafka/config/server.properties

echo "Starting kafka"
exec /opt/kafka/bin/kafka-server-start.sh /opt/kafka/config/server.properties
