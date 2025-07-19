package com.example.demo.projects.risk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:38:14
 * @Version 1.0
 */

@Service
public class RiskOrderProducerService {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendOrder(String orderId, String userId, double amount) {
        String orderJson = String.format("{\"orderId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f}", orderId, userId, amount);
        kafkaTemplate.send("risk-order", orderId, orderJson);
    }

    // 批量模拟
    public void batchSend() {
        String[] users = {"user123", "user789", "user456", "user888"};
        Random random = new Random();
        for (int i = 1; i <= 20; i++) {
            String userId = users[random.nextInt(users.length)];
            String orderId = "RISK" + i;
            double amount = Math.round((10 + random.nextDouble() * 990) * 100) / 100.0;
            sendOrder(orderId, userId, amount);
        }
    }
}
