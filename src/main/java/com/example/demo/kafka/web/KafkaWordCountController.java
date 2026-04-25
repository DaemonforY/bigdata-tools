package com.example.demo.kafka.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka 实时单词计数（轻量版，不依赖 Kafka Streams）
 *
 * GET  /kafka/wordcount/demo     → 模板
 * POST /kafka/wordcount/produce  → 发送一行文本
 * GET  /kafka/wordcount/counts   → 当前累计计数 Map
 * 推送：/topic/wordcount  每次更新后发整张表
 */
@Controller
@RequestMapping("/kafka/wordcount")
public class KafkaWordCountController {

    private static final String TOPIC = "kafka-wordcount-input";

    private final Map<String, AtomicLong> counts = new ConcurrentHashMap<>();

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private SimpMessagingTemplate ws;

    @GetMapping("/demo")
    public String demo() {
        return "kafka/wordcount_demo";
    }

    @PostMapping("/produce")
    @ResponseBody
    public String produce(@RequestParam String sentence) {
        kafkaTemplate.send(TOPIC, sentence);
        return "OK";
    }

    @GetMapping("/counts")
    @ResponseBody
    public Map<String, Long> counts() {
        Map<String, Long> snapshot = new TreeMap<>();
        counts.forEach((k, v) -> snapshot.put(k, v.get()));
        return snapshot;
    }

    @KafkaListener(topics = TOPIC, groupId = "kafka-wordcount-web")
    public void onMessage(String sentence) {
        for (String w : sentence.toLowerCase().split("\\W+")) {
            if (!w.isBlank()) {
                counts.computeIfAbsent(w, k -> new AtomicLong()).incrementAndGet();
            }
        }
        ws.convertAndSend("/topic/wordcount", counts());
    }
}
