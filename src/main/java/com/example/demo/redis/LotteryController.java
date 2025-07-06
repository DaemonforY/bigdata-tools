package com.example.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Set;

/**
 * @Description 3. 抽奖池（Set类型）
 * @Author miaoyongbin
 * @Date 2025/7/2 06:20:19
 * @Version 1.0
 */
@Controller
public class LotteryController {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/redis/lottery")
    public String lotteryPage(Model model) {
        Set<String> members = redisTemplate.opsForSet().members("lottery");
        model.addAttribute("members", members);
        return "redis/lottery";
    }

    @PostMapping("/redis/lottery/join")
    public String join(@RequestParam("name") String name) {
        redisTemplate.opsForSet().add("lottery", name);
        return "redirect:/redis/lottery";
    }

    @PostMapping("/lottery/draw")
    public String draw(Model model) {
        String winner = redisTemplate.opsForSet().randomMember("lottery");
        model.addAttribute("winner", winner);
        return "redis/lottery";
    }
}