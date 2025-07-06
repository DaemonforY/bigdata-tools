package com.example.demo.redis.demo;

import redis.clients.jedis.Jedis;

public class StringDemo {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost", 6379);
        jedis.set("name", "Alice");
        String name = jedis.get("name");
        jedis.incr("counter");
    }
}
