package com.example.demo.kafka.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 简单 Producer/Consumer 演示
 * GET  /kafka/simple/demo      → 模板
 * POST /kafka/simple/produce   → 生产 N 条消息（异步）
 * GET  /kafka/simple/history   → 最近 50 条消费历史
 * 实时推送：/topic/simple-progress/{sessionId}（生产进度）
 *           /topic/simple-consume（每条消费消息）
 */
@Controller
@EnableAsync
@RequestMapping("/kafka/simple")
public class KafkaSimpleController {

    private static final String TOPIC = "kafka-simple";

    /** 最近 50 条消费消息（FIFO） */
    private final Deque<String> history = new ConcurrentLinkedDeque<>();

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private SimpMessagingTemplate ws;

    @GetMapping("/demo")
    public String demo() {
        return "kafka/simple_demo";
    }

    @GetMapping("/history")
    @ResponseBody
    public List<String> history() {
        return new ArrayList<>(history);
    }

    @PostMapping("/produce")
    @ResponseBody
    public String produce(@RequestParam int count,
                          @RequestParam String template,
                          @RequestParam String wsSessionId) {
        produceAsync(count, template, wsSessionId);
        return "OK";
    }

    /** 后台异步发送，逐条推送进度，便于前端进度条 */
    @Async
    public void produceAsync(int count, String template, String wsSessionId) {
        long ts = System.currentTimeMillis();
        for (int i = 1; i <= count; i++) {
            String msg = template
                    .replace("{{i}}", String.valueOf(i))
                    .replace("{{ts}}", String.valueOf(System.currentTimeMillis()));
            kafkaTemplate.send(TOPIC, "k-" + i, msg);
            ws.convertAndSend(
                    "/topic/simple-progress/" + wsSessionId,
                    Map.of("sent", i, "total", count, "ts", ts));
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }
    }

    @KafkaListener(topics = TOPIC, groupId = "kafka-simple-web")
    public void onMessage(String value) {
        history.addFirst(value);
        while (history.size() > 50) history.pollLast();
        ws.convertAndSend("/topic/simple-consume", value);
    }
}
