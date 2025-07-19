package com.example.demo.projects.useraction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 06:55:58
 * @Version 1.0
 */

@Service
public class UserActionProducerService {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void sendUserAction(String userId, String url, long ts) {
        String msg = String.format("{\"userId\":\"%s\",\"url\":\"%s\",\"ts\":%d}", userId, url, ts);
        kafkaTemplate.send("user-action", userId, msg);
    }
}