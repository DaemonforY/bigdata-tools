package com.example.demo;

import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Set;

public class Tests {

    @Test
    public void counterTest(){
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            long count = jedis.incr("visit:counter");
            System.out.println("当前访问次数：" + count);
//            assertTrue(count > 0);
        } catch (Exception e) {
            e.printStackTrace();
            assert false : "连接 Redis 失败，请确保 Redis 服务已启动并监听 6379 端口";
        }
    }

    @Test
    public void lotteryTest(){
        Jedis jedis = new Jedis("localhost", 6379);
        jedis.sadd("lottery", "Alice", "Bob", "Charlie");
        String winner = jedis.srandmember("lottery");
        System.out.println("中奖者：" + winner);
        jedis.close();
    }

    @Test
    public void zsetTest(){
        Jedis jedis = new Jedis("localhost", 6379);
        jedis.zadd("rank", 100, "Tom");
        jedis.zadd("rank", 80, "Alice");
        jedis.zadd("rank", 90, "Bob");
        // Alice加10分
        jedis.zincrby("rank", 10, "Alice");
        // 查询前三名
        List<String> top3 = jedis.zrevrange("rank", 0, 2);
        System.out.println("排行榜前3名：" + top3);
        jedis.close();
    }
}
