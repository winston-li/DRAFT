(1) Build docker image
	docker build -t 192.168.200.64:5000/drama/kafka:v1 .
	docker push 192.168.200.64:5000/drama/kafka:v1
(2) Create kuberbetes pods & service
	kubectl create -f kafka.yaml [--namespace=xxx]
(3) Teardown
	./teardown.sh [--namespace=xxx]

Note:
(1) The image uses Oracle JRE 1.7 u51 & Kafka 0.8.2 (Scala 2.10)
	http://download.oracle.com/otn/java/jdk/7u51-b13/jre-7u51-linux-x64.tar.gz
	http://apache.stu.edu.tw/kafka/0.8.2.1/kafka_2.10-0.8.2.1.tgz

TODO:
(1) Kubernetes 1.0.x doesn't support emptyDir volumes for containers running as non-root (it's commit in master branch, not v1.0.0 branch, refer to https://github.com/kubernetes/kubernetes/pull/9384 & https://github.com/kubernetes/kubernetes/issues/12627). Use root rather than kafka user instead at this moment.
