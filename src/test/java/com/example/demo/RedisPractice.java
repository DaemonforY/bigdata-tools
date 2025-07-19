package com.example.demo;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.resps.Tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisPractice {
    @Test
    public void testSET() {
        Jedis jedis = new Jedis("192.168.10.102", 6379);
        jedis.sadd("tags", "java", "redis", "mysql", "redis", "redis");

        System.out.println(jedis.sismember("tags", "java"));
        jedis.srem("tags", "java");
        Set<String> tags = jedis.smembers("tags");
        System.out.println(tags);
        jedis.close();
    }

    @Test
    public void testHash() {
        Jedis jedis = new Jedis("192.168.10.102", 6379);
        Map<String, String> userData = new HashMap<>();
        userData.put("name", "Alice");
        userData.put("age", "19");
        jedis.hmset("user1", userData);
        Map<String, String> user = jedis.hgetAll("user1");
        System.out.println(user);
        jedis.close();
    }

    @Test
    public void testZSET() {
        Jedis jedis = new Jedis("192.168.10.102", 6379);
        jedis.zadd("ranking", 100, "Tom");
        jedis.zadd("ranking", 90, "Jerry");
        List<Tuple> top = jedis.zrevrangeWithScores("ranking", 0, 2);
        System.out.println(top);
        jedis.close();

    }

    @Test
    public void counter() {
        Jedis jedis = new Jedis("192.168.10.102", 6379);
        long counter = jedis.incr("visit:counter");
        System.out.println("当前访问人数:" + counter);
        jedis.close();
    }

    @Test
    public void quene() {
        Jedis jedis = new Jedis("192.168.10.102", 6379);
        jedis.lpush("kafka", "hello", "world");
        jedis.lpush("kafka", "hello", "world2");
        jedis.rpop("kafka");
        List mylist = jedis.lrange("kafka", 0, -1);
        System.out.println(mylist);
    }

    @Test
    public void lottery() {
        Jedis jedis = new Jedis("192.168.10.102", 6379);
        jedis.sadd("lottery", "java", "redis", "mysql", "redis", "redis");
        String lottery = jedis.srandmember("lottery");
        System.out.println("恭喜中奖" + lottery);
        jedis.close();
    }

    @Test
    public void rank() {
        Jedis jedis = new Jedis("192.168.10.102", 6379);
        Map<String, Double> map = new HashMap<>();
        map.put("alex", 100.0);
        map.put("zhangsan", 90.0);
        map.put("lisi", 91.1);
        map.put("wangwu", 92.3);
        map.put("wang", 93.9);

        jedis.zadd("rank", map);
        jedis.zincrby("rank", 6, "wang");
        List<Tuple> rank = jedis.zrevrangeWithScores("rank", 0, 2);
        System.out.println(rank);
        jedis.close();
    }

    @Test
    public void ttl() {
        Jedis jedis = new Jedis("192.168.10.102", 6379);
//        jedis.set("key","nihao shijie");
//        jedis.expire("key",5);
        System.out.println(jedis.ttl("key"));
        System.out.println(jedis.ttl("key"));
        System.out.println(jedis.ttl("key"));
        System.out.println(jedis.ttl("key"));
        System.out.println(jedis.ttl("key"));
        System.out.println(jedis.ttl("key"));
        System.out.println(jedis.ttl("key"));
        jedis.close();
    }

    @Test
    public void sub() {
        Jedis jedis = new Jedis("192.168.10.102", 6379);
        jedis.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                System.out.println("收到消息: " + message);
            }
        }, "news");

// 发布端
        Jedis jedisPub = new Jedis("192.168.10.102", 6379);
        jedisPub.publish("news", "hello redis");

    }

    @Test
    public void session() {
        Jedis jedis = new Jedis("192.168.10.102", 6379);
        Transaction tx = jedis.multi();
        tx.incr("counter");
        tx.incr("counter");
        tx.exec();
        System.out.println(jedis.get("counter"));
        jedis.close();
    }
}
