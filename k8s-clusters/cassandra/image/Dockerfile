FROM centos:latest

ENV CASSANDRA_VERSION 2.1.8
EXPOSE 9042 9160

RUN yum update -y
RUN rpm --rebuilddb && yum install -y install wget tar && yum clean all \
    && wget -q -O - --no-cookies --no-check-certificate --header "Cookie: oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u45-b14/jre-8u45-linux-x64.tar.gz" | tar -xzf - -C /opt \
    && wget -q -O - http://downloads.datastax.com/community/dsc-cassandra-${CASSANDRA_VERSION}-bin.tar.gz | tar -xzf - -C /etc \
    && mv /etc/dsc-cassandra-${CASSANDRA_VERSION} /etc/cassandra 
RUN alternatives --install /usr/bin/java java /opt/jre1.8.0_45/bin/java 1

ENV PATH /etc/cassandra/bin:$PATH
WORKDIR /etc/cassandra    

COPY run.sh ./bin/
COPY kubernetes-cassandra.jar ./lib/
COPY cassandra.yaml ./conf/
COPY log4j-server.properties ./conf/

RUN groupadd cassandra && \
    useradd -g cassandra cassandra

RUN chmod a+x ./bin/run.sh && \
    mkdir -p /cassandra_data/{data,commitlog,saved_caches,log} && \
    chown -R cassandra.cassandra /etc/cassandra /cassandra_data && \
    chmod o+w -R /etc/cassandra /cassandra_data

VOLUME ["/cassandra_data/data","/cassandra_data/commitlog","/cassandra_data/saved_caches","/cassandra_data/log"]    

# Comment below out due to Kubernetes v1.0.x doesn't support emptyDir volumes for containers running as non-root
#USER cassandra

CMD ["/etc/cassandra/bin/run.sh"]