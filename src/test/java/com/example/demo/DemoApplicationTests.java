package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.Jedis;

@SpringBootTest
class DemoApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void counterDemo(){
        Jedis jedis = new Jedis("localhost", 6379);
        long count = jedis.incr("visit:counter");
        System.out.println("当前访问次数：" + count);
        jedis.close();
    }

}
