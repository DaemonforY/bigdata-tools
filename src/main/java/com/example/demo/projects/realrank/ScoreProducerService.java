package com.example.demo.projects.realrank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:45:03
 * @Version 1.0
 */

@Service
public class ScoreProducerService {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendScore(String userId, int score, long ts) {
        String msg = String.format("{\"userId\":\"%s\",\"score\":%d,\"ts\":%d}", userId, score, ts);
        kafkaTemplate.send("score", userId, msg);
    }

    // 批量模拟
    public void batchSend() {
        String[] users = {"alice", "bob", "carol", "dave", "eve"};
        Random random = new Random();
        for (int i = 1; i <= 50; i++) {
            String userId = users[random.nextInt(users.length)];
            int score = random.nextInt(10) + 1;
            long ts = System.currentTimeMillis();
            sendScore(userId, score, ts);
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }
    }
}
