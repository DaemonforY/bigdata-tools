package com.example.demo.kafka.demo;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.Callback;
import java.util.Properties;
/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/2 09:38:56
 * @Version 1.0
 */
public class KafkaProducerDemo {
    /** 默认连本地 docker kafka，可通过 -Dkafka.bootstrap.servers=... 覆盖 */
    private static final String BOOTSTRAP_SERVERS =
            System.getProperty("kafka.bootstrap.servers",
                    System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));

    /** 演示发送的 topic，可通过 -Dkafka.topic=... 覆盖 */
    private static final String TOPIC = System.getProperty("kafka.topic", "test3");

    public static void main(String[] args) throws InterruptedException {
        try (KafkaProducer<String, String> producer = buildProducer()) {
            for (int i = 1; i <= 100; i++) {
                String key = "key" + i;
                String value = "message-" + i;
                Thread.sleep(50); // 节流到 ~20 条/秒，便于观察

                producer.send(new ProducerRecord<>(TOPIC, key, value), (metadata, exception) -> {
                    if (exception == null) {
                        System.out.printf("Sent: %s, partition: %d, offset: %d%n",
                                value, metadata.partition(), metadata.offset());
                    } else {
                        exception.printStackTrace();
                    }
                });
            }
            producer.flush();
        }
    }

    private static KafkaProducer<String, String> buildProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all"); // 等所有副本确认（单节点也够稳）
        props.put("retries", 3);
        return new KafkaProducer<>(props);
    }
}