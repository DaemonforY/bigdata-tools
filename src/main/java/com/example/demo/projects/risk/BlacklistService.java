package com.example.demo.projects.risk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:37:48
 * @Version 1.0
 */
@Service
public class BlacklistService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void add(String userId) {
        try {
            stringRedisTemplate.opsForSet().add("blacklist", userId);
        } catch (DataAccessException e) {
            System.err.println("Redis 不可用，无法添加黑名单: " + e.getMessage());
        }
    }
    public void remove(String userId) {
        try {
            stringRedisTemplate.opsForSet().remove("blacklist", userId);
        } catch (DataAccessException e) {
            System.err.println("Redis 不可用，无法移除黑名单: " + e.getMessage());
        }
    }
    public Set<String> getAll() {
        try {
            Set<String> result = stringRedisTemplate.opsForSet().members("blacklist");
            return result == null ? Collections.emptySet() : result;
        } catch (DataAccessException e) {
            System.err.println("Redis 不可用，无法查询黑名单: " + e.getMessage());
            return Collections.emptySet();
        }
    }
    public boolean isBlack(String userId) {
        try {
            Boolean result = stringRedisTemplate.opsForSet().isMember("blacklist", userId);
            return Boolean.TRUE.equals(result);
        } catch (DataAccessException e) {
            System.err.println("Redis 不可用，无法校验黑名单: " + e.getMessage());
            return false;
        }
    }
}
