package com.example.demo.kafka.demo.streams;


import org.apache.kafka.clients.producer.*;
import java.util.Properties;
/**
 * @Description 3. Kafka Streams 实时单词计数
 * Producer（生成英文句子）
 * @Author miaoyongbin
 * @Date 2025/7/2 10:10:36
 * @Version 1.0
 */
public class WordProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "39.98.123.172:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        String[] sentences = {
                "Kafka Streams is powerful",
                "Kafka is a distributed streaming platform",
                "Streams API is easy to use",
                "Hello Kafka"
        };

        for (String sentence : sentences) {
            producer.send(new ProducerRecord<>("input-topic", sentence));
            System.out.println("发送句子: " + sentence);
        }
        producer.close();
    }
}