package com.example.demo.redis;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Description 1. 计数器（String类型）
 * @Author miaoyongbin
 * @Date 2025/7/2 06:02:45
 * @Version 1.0
 */
@Controller
@Tag(name = "计数器")
public class CounterController {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/redis/counter")
    @Operation(summary = "计数器")
    public String counter(Model model) {
        Long count = redisTemplate.opsForValue().increment("visit:counter");
        model.addAttribute("count", count);
        return "redis/counter";
    }
}

