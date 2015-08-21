FROM centos:latest

ENV KAFKA_VERSION 0.8.2.1
ENV SCALA_VERSION 2.10
ENV KAFKA_RELEASE_TAG kafka_${SCALA_VERSION}-${KAFKA_VERSION}
EXPOSE 9092

RUN yum update -y
RUN rpm --rebuilddb && yum install -y install wget tar && yum clean all \
    && wget -q -O - --no-cookies --no-check-certificate --header "Cookie: oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/7u51-b13/jre-7u51-linux-x64.tar.gz" | tar -xzf - -C /opt \
    && wget -q -O - http://apache.stu.edu.tw/kafka/${KAFKA_VERSION}/${KAFKA_RELEASE_TAG}.tgz | tar -xzf - -C /opt \
    && mv /opt/${KAFKA_RELEASE_TAG} /opt/kafka 
RUN alternatives --install /usr/bin/java java /opt/jre1.7.0_51/bin/java 1

ENV PATH /opt/kafka/bin:$PATH
WORKDIR /opt/kafka

COPY config-and-run.sh ./bin/
COPY server.properties ./config/ 
COPY log4j.properties ./config/
#ADD http://repo1.maven.org/maven2/org/slf4j/slf4j-log4j12/1.7.6/slf4j-log4j12-1.7.6.jar /kafka/libs/

RUN groupadd kafka && \
    useradd -g kafka kafka

RUN chmod a+x ./bin/config-and-run.sh && \
    mkdir -p /kafka_data/{data,log} && \
    chown -R kafka.kafka /opt/kafka /kafka_data && \
    chmod o+w -R /opt/kafka /kafka_data

VOLUME [ "/kafka_data/data", "/kafka_data/log" ]

#USER kafka

CMD ["/opt/kafka/bin/config-and-run.sh"]