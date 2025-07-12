package com.example.demo.kafka.demo;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.*;
/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/2 03:51:39
 * @Version 1.0
 */
public class KafkaConsumerExercise {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "39.98.123.172:9092");
        props.put("group.id", "demo-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("enable.auto.commit", "false"); // 关闭自动提交

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("test3"));

        int totalCount = 0;
        Map<Integer, Integer> partitionCount = new HashMap<>();

        long lastPrintTime = System.currentTimeMillis();
        int countThisSecond = 0;

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    totalCount++;
                    countThisSecond++;
                    int partition = record.partition();
                    partitionCount.put(partition, partitionCount.getOrDefault(partition, 0) + 1);

                    System.out.printf("partition=%d, offset=%d, key=%s, value=%s, total=%d%n",
                            partition, record.offset(), record.key(), record.value(), totalCount);
                }

                // 手动提交offset
                consumer.commitSync();

                // 每秒打印一次速率和分区统计
                long now = System.currentTimeMillis();
                if (now - lastPrintTime >= 1000) {
                    System.out.println("---- 每秒消费条数: " + countThisSecond);
                    System.out.println("---- 各分区累计消息数: " + partitionCount);
                    countThisSecond = 0;
                    lastPrintTime = now;
                }
            }
        } finally {
            consumer.close();
        }
    }
}