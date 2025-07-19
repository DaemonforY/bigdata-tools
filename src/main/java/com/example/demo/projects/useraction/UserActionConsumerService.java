package com.example.demo.projects.useraction;

import com.example.demo.hbase.HBaseUtil;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.json.JSONObject;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 06:56:28
 * @Version 1.0
 */
@Service
public class UserActionConsumerService {
    @KafkaListener(topics = "user-action", groupId = "useraction-group")
    public void consume(String msg) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            JSONObject obj = new JSONObject(msg);
            String userId = obj.getString("userId");
            String url = obj.getString("url");
            long ts = obj.getLong("ts");

            // Redis计数器
            jedis.incr("visit:" + userId);

            // HBase存日志
            Table table = HBaseUtil.getTable("user_action");
            String rowKey = userId + "_" + ts;
            Put put = new Put(rowKey.getBytes());
            put.addColumn("info".getBytes(), "url".getBytes(), url.getBytes());
            put.addColumn("info".getBytes(), "ts".getBytes(), String.valueOf(ts).getBytes());
            table.put(put);
            table.close();

            System.out.printf("已消费并写入：userId=%s, url=%s, ts=%d%n", userId, url, ts);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
