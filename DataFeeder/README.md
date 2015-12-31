### Steps

    1. java [-DKAFKA_BROKERS=ip1:9092,ip2:9092,...] [-Dconfig.file=filepath_of_specified_conf_file] -cp KafkaDataFeederAssembly-0.1.0.jar com.compal.drama.draft.DataFeeder
    2. check kafka topic via kafka CLI: bin/kafka-console-consumer.sh --zookeeper ip:2181 --topic weather.raw
