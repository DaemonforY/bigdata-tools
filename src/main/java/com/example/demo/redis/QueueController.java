package com.example.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @Description  2. 简单消息队列（List类型）
 * @Author miaoyongbin
 * @Date 2025/7/2 06:16:09
 * @Version 1.0
 */
@Controller
public class QueueController {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/redis/queue")
    public String queuePage(Model model) {
        List<String> msgs = redisTemplate.opsForList().range("queue:msg", 0, -1);
        model.addAttribute("msgs", msgs);
        return "redis/queue";
    }

    @PostMapping("/redis/queue/push")
    public String push(@RequestParam("msg") String msg) {
        redisTemplate.opsForList().leftPush("queue:msg", msg);
        return "redirect:/redis/queue";
    }

    @PostMapping("/redis/queue/pop")
    public String pop() {
        redisTemplate.opsForList().rightPop("queue:msg");
        return "redirect:/redis/queue";
    }
}