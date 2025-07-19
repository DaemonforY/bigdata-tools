package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * @Description WebSocket全局配置，支持/simple和/order两个endpoint
 * @Author miaoyongbin
 * @Date 2025/7/4
 * @Version 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 支持 /ws/simple
        registry.addEndpoint("/ws/simple").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/simple").setAllowedOriginPatterns("*").withSockJS();
        // 支持 /ws/order
        registry.addEndpoint("/ws/order").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/order").setAllowedOriginPatterns("*").withSockJS();
        // 支持 /ws/wordcount
        registry.addEndpoint("/ws/wordcount").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/wordcount").setAllowedOriginPatterns("*").withSockJS();

        registry.addEndpoint("/ws/partition").setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws/partition").setAllowedOriginPatterns("*").withSockJS();

        registry.addEndpoint("/ws/score").setAllowedOriginPatterns("*").withSockJS();

    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 统一使用 /topic 前缀
        registry.enableSimpleBroker("/topic");
    }
}