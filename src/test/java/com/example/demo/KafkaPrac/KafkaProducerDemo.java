package com.example.demo.KafkaPrac;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;

public class KafkaProducerDemo {
    public static void main(String[] args) throws InterruptedException {
        // 1. 配置Producer参数
        KafkaProducer<String, String> producer = getStringStringKafkaProducer();

        // 3. 发送100条消息
        for (int i = 1; i <= 100; i++) {
            String key = "key" + i;
            String value = "我是message-" + i;
            Thread.sleep(500);

            // --- 同步发送 ---

            try {
                RecordMetadata metadata = producer.send(new ProducerRecord<>("test", key, value)).get();
                System.out.printf("Sent: %s, partition: %d, offset: %d%n", value, metadata.partition(), metadata.offset());
            } catch (Exception e) {
                e.printStackTrace();
            }


            // --- 异步发送 ---
//            producer.send(new ProducerRecord<>("test", key, value), new Callback() {
//                @Override
//                public void onCompletion(RecordMetadata metadata, Exception exception) {
//                    if (exception == null) {
//                        System.out.printf("Sent: %s, partition: %d, offset: %d%n", value, metadata.partition(), metadata.offset());
//                    } else {
//                        exception.printStackTrace();
//                    }
//                }
//            });
        }

        // 4. 关闭Producer
        producer.close();
    }

    private static KafkaProducer<String, String> getStringStringKafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "hadoop102:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        // 可选：提高可靠性
        props.put("acks", "all"); // 等待所有副本确认
        props.put("retries", 3);  // 失败重试次数

        // 2. 创建Producer实例
        return new KafkaProducer<>(props);
    }
}
