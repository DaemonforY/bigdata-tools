package com.example.demo.kafka.demo.streams;


import org.apache.kafka.clients.consumer.*;
import java.time.Duration;
import java.util.*;
/**
 * @Description Consumer（统计结果）
 * @Author miaoyongbin
 * @Date 2025/7/2 10:12:02
 * @Version 1.0
 */
public class WordCountConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "39.98.123.172:9092");
        props.put("group.id", "wordcount-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.LongDeserializer");

        KafkaConsumer<String, Long> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("output-topic"));

        while (true) {
            ConsumerRecords<String, Long> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, Long> record : records) {
                System.out.printf("单词: %s, 次数: %d%n", record.key(), record.value());
            }
        }
    }
}