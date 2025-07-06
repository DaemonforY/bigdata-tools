package com.example.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @Description 6. 进阶：接口限流（每分钟最多访问5次）
 * @Author miaoyongbin
 * @Date 2025/7/2 06:25:40
 * @Version 1.0
 */
@Controller
public class RateLimitController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/redis/api/visitPage")
    public String visitPage() {
        return "redis/rate_limit";
    }

    @GetMapping("/redis/api/visit")
    @ResponseBody
    public String visit(@RequestParam("user") String user) {
        String key = "req:user:" + user;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, java.time.Duration.ofSeconds(60));
        }
        if (count > 5) {
            return "访问过于频繁！";
        }
        return "允许访问，第" + count + "次";
    }
}