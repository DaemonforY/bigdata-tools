package com.example.demo.projects.order;

import com.example.demo.hbase.HBaseUtil;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:18:42
 * @Version 1.0
 */

@Service
public class OrderQueryService {
    public String getOrderStatusFromRedis(String orderId) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            return jedis.get("order:status:" + orderId);
        }
    }

    public Map<String, Object> getOrderStatusFromHBase(String orderId) {
        Map<String, Object> resultMap = new HashMap<>();
        try (Table table = HBaseUtil.getTable("order")) {
            Get get = new Get(orderId.getBytes());
            Result result = table.get(get);

            byte[] latest = result.getValue("status".getBytes(), "latest".getBytes());
            resultMap.put("latest", latest == null ? null : new String(latest));

            NavigableMap<byte[], byte[]> familyMap = result.getFamilyMap("status".getBytes());
            List<Map<String, String>> history = new ArrayList<>();
            if (familyMap != null) {
                for (Map.Entry<byte[], byte[]> entry : familyMap.entrySet()) {
                    String col = new String(entry.getKey());
                    String val = new String(entry.getValue());
                    if (col.startsWith("history_")) {
                        Map<String, String> map = new HashMap<>();
                        map.put("ts", col.substring(8));
                        map.put("status", val);
                        history.add(map);
                    }
                }
            }
            // 按时间倒序
            history.sort((a, b) -> b.get("ts").compareTo(a.get("ts")));
            resultMap.put("history", history);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }
}
