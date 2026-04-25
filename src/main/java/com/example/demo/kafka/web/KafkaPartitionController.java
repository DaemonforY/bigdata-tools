package com.example.demo.kafka.web;

import jakarta.annotation.PreDestroy;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 分区与 Rebalance 演示
 *
 * GET  /kafka/partition/demo                → 模板
 * POST /kafka/partition/produce?count=N      → 生产 N 条消息（轮询不指定 key）
 * POST /kafka/partition/start?consumerId=X   → 启动一个消费者，加入同一 group
 * POST /kafka/partition/stop?consumerId=X    → 停止指定消费者
 *
 * 每个动态消费者的消息推送到 /topic/partition-demo/{consumerId}
 *
 * 教学要点：在多个消费者启动/停止时，前端能看到分区被重新分配（rebalance）。
 */
@Controller
@RequestMapping("/kafka/partition")
public class KafkaPartitionController {

    private static final String TOPIC = "kafka-partition-demo";
    private static final String GROUP = "kafka-partition-web";
    private static final int PARTITIONS = 3;

    private final Map<String, ConsumerWorker> workers = new ConcurrentHashMap<>();

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private SimpMessagingTemplate ws;

    @Value("${spring.kafka.bootstrap-servers}") String bootstrap;

    @GetMapping("/demo")
    public String demo() {
        return "kafka/partition_demo";
    }

    @PostMapping("/produce")
    @ResponseBody
    public String produce(@RequestParam(defaultValue = "30") int count) {
        for (int i = 1; i <= count; i++) {
            // 不指定 key,让 producer 轮询/粘性分区
            kafkaTemplate.send(TOPIC, null, "msg-" + i + "-ts" + System.currentTimeMillis());
        }
        return "OK";
    }

    @PostMapping("/start")
    @ResponseBody
    public String start(@RequestParam String consumerId) {
        workers.computeIfAbsent(consumerId, id -> {
            ConsumerWorker w = new ConsumerWorker(id, bootstrap, ws);
            w.start();
            return w;
        });
        return "OK";
    }

    @PostMapping("/stop")
    @ResponseBody
    public String stop(@RequestParam String consumerId) {
        ConsumerWorker w = workers.remove(consumerId);
        if (w != null) w.shutdown();
        return "OK";
    }

    @PreDestroy
    public void cleanup() {
        workers.values().forEach(ConsumerWorker::shutdown);
        workers.clear();
    }

    /** 单个消费者线程：拉消息 → WebSocket 推送 */
    static class ConsumerWorker {
        private final String consumerId;
        private final Thread thread;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final KafkaConsumer<String, String> consumer;

        ConsumerWorker(String consumerId, String bootstrap, SimpMessagingTemplate ws) {
            this.consumerId = consumerId;
            Properties p = new Properties();
            p.put("bootstrap.servers", bootstrap);
            p.put("group.id", GROUP);
            p.put("client.id", consumerId);
            p.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            p.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            p.put("auto.offset.reset", "latest");
            this.consumer = new KafkaConsumer<>(p);
            this.consumer.subscribe(List.of(TOPIC));

            this.thread = new Thread(() -> {
                try {
                    while (running.get()) {
                        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                        records.forEach(r -> ws.convertAndSend(
                                "/topic/partition-demo/" + consumerId,
                                Map.of("partition", r.partition(),
                                        "offset", r.offset(),
                                        "value", r.value())));
                    }
                } finally {
                    consumer.close();
                }
            }, "kafka-partition-" + consumerId);
            thread.setDaemon(true);
        }

        void start() { thread.start(); }
        void shutdown() {
            running.set(false);
            consumer.wakeup();
        }
    }
}
