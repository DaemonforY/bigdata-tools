package com.example.demo.projects.risk;

import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.Set;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:37:48
 * @Version 1.0
 */
@Service
public class BlacklistService {
    public void add(String userId) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            jedis.sadd("blacklist", userId);
        }
    }
    public void remove(String userId) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            jedis.srem("blacklist", userId);
        }
    }
    public Set<String> getAll() {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            return jedis.smembers("blacklist");
        }
    }
    public boolean isBlack(String userId) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            return jedis.sismember("blacklist", userId);
        }
    }
}
