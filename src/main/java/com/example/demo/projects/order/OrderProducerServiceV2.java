package com.example.demo.projects.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.Random;

/**
 * @Description 订单生产者服务
 * @Author miaoyongbin
 * @Date 2025/7/4 07:17:38
 * @Version 1.0
 */

@Service
public class OrderProducerServiceV2 {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendOrder(String orderId, String userId, double amount) {
        String orderJson = String.format("{\"orderId\":\"%s\",\"userId\":\"%s\",\"amount\":%.2f,\"action\":\"CREATE\"}", orderId, userId, amount);
        kafkaTemplate.send("order", orderId, orderJson);
    }

    public void changeOrderStatus(String orderId, String status) {
        String eventJson = String.format("{\"orderId\":\"%s\",\"action\":\"STATUS\",\"status\":\"%s\"}", orderId, status);
        kafkaTemplate.send("order", orderId, eventJson);
    }

    // 可选：批量生成伪数据
    public void batchSendOrders(int count) {
        Random random = new Random();
        for (int i = 1; i <= count; i++) {
            String orderId = "O" + (1000 + i);
            String userId = "user" + (random.nextInt(10) + 1);
            double amount = Math.round((10 + random.nextDouble() * 990) * 100) / 100.0;
            sendOrder(orderId, userId, amount);
        }
    }
}
