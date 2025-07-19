package com.example.demo.projects.order;

import com.example.demo.hbase.HBaseUtil;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:18:07
 * @Version 1.0
 */

@Service
public class OrderConsumerServiceV2 {
    @Autowired
    private SimpMessagingTemplate wsTemplate;

    @KafkaListener(topics = "order", groupId = "order-group")
    public void consume(String msg) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            JSONObject obj = new JSONObject(msg);
            String orderId = obj.getString("orderId");
            String action = obj.optString("action", "CREATE");
            long ts = System.currentTimeMillis();

            Table table = HBaseUtil.getTable("order");

            if ("CREATE".equals(action)) {
                String userId = obj.getString("userId");
                double amount = obj.getDouble("amount");
                String status = "PAID";
                // HBase
                Put put = new Put(orderId.getBytes());
                put.addColumn("info".getBytes(), "userId".getBytes(), userId.getBytes());
                put.addColumn("info".getBytes(), "amount".getBytes(), String.valueOf(amount).getBytes());
                put.addColumn("status".getBytes(), "latest".getBytes(), status.getBytes());
                put.addColumn("status".getBytes(), ("history_" + ts).getBytes(), status.getBytes());
                table.put(put);
                // Redis
                jedis.setex("order:status:" + orderId, 300, status);
                wsTemplate.convertAndSend("/topic/order-status/" + orderId, status);
            } else if ("STATUS".equals(action)) {
                String status = obj.getString("status");
                // HBase
                Put put = new Put(orderId.getBytes());
                put.addColumn("status".getBytes(), "latest".getBytes(), status.getBytes());
                put.addColumn("status".getBytes(), ("history_" + ts).getBytes(), status.getBytes());
                table.put(put);
                // Redis
                jedis.setex("order:status:" + orderId, 300, status);
                wsTemplate.convertAndSend("/topic/order-status/" + orderId, status);
            }
            table.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
