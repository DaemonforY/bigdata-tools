package com.example.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * @Description  5. 排行榜（ZSet类型）
 * @Author miaoyongbin
 * @Date 2025/7/2 06:24:43
 * @Version 1.0
 */
@Controller
public class RankController {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/redis/rank")
    public String rankPage(Model model) {
        Set<String> top3 = redisTemplate.opsForZSet().reverseRange("rank", 0, 2);
        Map<String, Double> scores = new LinkedHashMap<>();
        if (top3 != null) {
            for (String user : top3) {
                Double score = redisTemplate.opsForZSet().score("rank", user);
                scores.put(user, score);
            }
        }
        model.addAttribute("scores", scores);
        return "redis/rank";
    }

    @PostMapping("/redis/rank/add")
    public String add(@RequestParam("user") String user, @RequestParam("score") double score) {
        redisTemplate.opsForZSet().incrementScore("rank", user, score);
        return "redirect:/redis/rank";
    }
}