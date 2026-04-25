package com.example.demo.kafka.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 延迟消息演示（教学用：基于时间戳的简单延迟）
 *
 * 真实业务里延迟队列通常用 Redis ZSET / RocketMQ / 自建 scheduler；
 * 这里为了演示原理，用最朴素的方式：
 * 1) 生产时把 deliverAt 写进消息内容
 * 2) 消费者收到所有消息但只把 "已到期" 的标为 consumed
 * 3) 历史接口返回所有挂起 + 已到期的状态，前端做倒计时
 */
@Controller
@EnableScheduling
@RequestMapping("/kafka/delay")
public class KafkaDelayController {

    private static final String TOPIC = "kafka-delay-demo";

    /** orderId -> 消息状态 */
    private final Map<String, DelayMsg> store = new ConcurrentHashMap<>();

    @Autowired private KafkaTemplate<String, String> kafkaTemplate;

    @GetMapping("/send")
    public String page() {
        return "kafka/delay_send";
    }

    @PostMapping("/send")
    @ResponseBody
    public String produce(@RequestParam String orderId,
                          @RequestParam int delaySeconds) {
        long deliverAt = System.currentTimeMillis() + delaySeconds * 1000L;
        String payload = orderId + "|" + deliverAt;
        kafkaTemplate.send(TOPIC, orderId, payload);
        return "OK";
    }

    @GetMapping("/history")
    @ResponseBody
    public List<DelayMsg> history() {
        // 倒序返回最近 20 条
        List<DelayMsg> list = new ArrayList<>(store.values());
        list.sort(Comparator.comparingLong((DelayMsg m) -> m.deliverTime).reversed());
        if (list.size() > 20) list = list.subList(0, 20);
        return list;
    }

    @KafkaListener(topics = TOPIC, groupId = "kafka-delay-web")
    public void onMessage(String value) {
        String[] parts = value.split("\\|", 2);
        if (parts.length != 2) return;
        String orderId = parts[0];
        long deliverAt = Long.parseLong(parts[1]);
        store.put(orderId, new DelayMsg(orderId, deliverAt, 0));
    }

    /** 每秒扫描：到期的标为已消费 */
    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 1000)
    public void tick() {
        long now = System.currentTimeMillis();
        store.values().forEach(m -> {
            if (m.actualConsumeTime == 0 && now >= m.deliverTime) {
                m.actualConsumeTime = now;
            }
        });
    }

    public static class DelayMsg {
        public String orderId;
        public long deliverTime;        // 计划触达时间
        public long actualConsumeTime;  // 0 = 未到期；>0 = 已"消费"

        public DelayMsg() {}
        public DelayMsg(String orderId, long deliverTime, long actualConsumeTime) {
            this.orderId = orderId;
            this.deliverTime = deliverTime;
            this.actualConsumeTime = actualConsumeTime;
        }
    }
}
