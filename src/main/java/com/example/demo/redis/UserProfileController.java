package com.example.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @Description 4. 用户Profile（Hash类型）
 * @Author miaoyongbin
 * @Date 2025/7/2 06:23:14
 * @Version 1.0
 */
@Controller
public class UserProfileController {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/redis/profile")
    public String profilePage(Model model) {
        Map<Object, Object> profile = redisTemplate.opsForHash().entries("user:1001");
        model.addAttribute("profile", profile);
        return "redis/profile";
    }

    @PostMapping("/redis/profile/save")
    public String save(@RequestParam("name") String name, @RequestParam("age") String age) {
        redisTemplate.opsForHash().put("user:1001", "name", name);
        redisTemplate.opsForHash().put("user:1001", "age", age);
        return "redirect:/redis/profile";
    }
}