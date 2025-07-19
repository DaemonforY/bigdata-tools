package com.example.demo.projects.risk;

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
 * @Date 2025/7/4 07:38:37
 * @Version 1.0
 */

@Service
public class RiskOrderConsumerService {
    @KafkaListener(topics = "risk-order", groupId = "risk-group")
    public void consume(String msg) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            JSONObject order = new JSONObject(msg);
            String orderId = order.getString("orderId");
            String userId = order.getString("userId");

            boolean isBlack = jedis.sismember("blacklist", userId);
            String status = isBlack ? "BLOCKED" : "NORMAL";

            Table table = HBaseUtil.getTable("order_audit");
            Put put = new Put(orderId.getBytes());
            put.addColumn("info".getBytes(), "userId".getBytes(), userId.getBytes());
            put.addColumn("info".getBytes(), "status".getBytes(), status.getBytes());
            put.addColumn("info".getBytes(), "detail".getBytes(), order.toString().getBytes());
            table.put(put);
            table.close();

            System.out.printf("订单%s, 用户%s, 状态: %s%n", orderId, userId, status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
