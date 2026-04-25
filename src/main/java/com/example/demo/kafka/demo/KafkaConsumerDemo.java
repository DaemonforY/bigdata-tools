package com.example.demo.kafka.demo;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/2 09:46:54
 * @Version 1.0
 */
public class KafkaConsumerDemo {

    private static final String BOOTSTRAP_SERVERS =
            System.getProperty("kafka.bootstrap.servers",
                    System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092"));

    /** 默认订阅 test3，与 KafkaProducerDemo 一致；可通过 -Dkafka.topic=... 覆盖 */
    private static final String TOPIC = System.getProperty("kafka.topic", "test3");

    public static void main(String[] args) {
        try (KafkaConsumer<String, String> consumer = buildConsumer()) {
            consumer.subscribe(List.of(TOPIC));
            int totalCount = 0;
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    totalCount++;
                    System.out.printf("offset = %d, key = %s, value = %s, total = %d%n",
                            record.offset(), record.key(), record.value(), totalCount);
                }
            }
        }
    }

    private static KafkaConsumer<String, String> buildConsumer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        props.put("group.id", "demo-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        // 第一次启动从最早开始消费，便于课堂演示
        props.put("auto.offset.reset", "earliest");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        return new KafkaConsumer<>(props);
    }
}