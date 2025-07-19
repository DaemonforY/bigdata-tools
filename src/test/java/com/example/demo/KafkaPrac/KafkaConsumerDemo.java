package com.example.demo.KafkaPrac;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class KafkaConsumerDemo {
    public static void main(String[] args) {
        // 1. 配置Consumer参数
        KafkaConsumer<String, String> consumer = getStringStringKafkaConsumer();

        // 3. 消费消息并统计总数
        try (consumer) {
            consumer.subscribe(List.of("test"));
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

    private static KafkaConsumer<String, String> getStringStringKafkaConsumer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "hadoop102:9092");
        props.put("group.id", "demo-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        // 自动提交offset（可选）
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");

        // 2. 创建Consumer实例并订阅topic
        return new KafkaConsumer<>(props);
    }
}
