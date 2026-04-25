package com.example.demo.redis.demo;

import org.springframework.data.redis.core.StringRedisTemplate;

public class StringDemo {
    private final StringRedisTemplate stringRedisTemplate;

    public StringDemo(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void run() {
        stringRedisTemplate.opsForValue().set("name", "Alice");
        String name = stringRedisTemplate.opsForValue().get("name");
        stringRedisTemplate.opsForValue().increment("counter");
        System.out.println(name);
    }
}
