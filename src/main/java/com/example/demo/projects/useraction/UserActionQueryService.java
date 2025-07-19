package com.example.demo.projects.useraction;

import com.example.demo.hbase.HBaseUtil;
import org.apache.hadoop.hbase.client.*;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * @Description 需提前创建HBASE表：create 'user_action', 'info'
 * @Author miaoyongbin
 * @Date 2025/7/4 06:57:08
 * @Version 1.0
 */
@Service
public class UserActionQueryService {
    public long getRedisCount(String userId) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            String v = jedis.get("visit:" + userId);
            return v == null ? 0 : Long.parseLong(v);
        }
    }

    public List<Map<String, Object>> getUserActions(String userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Table table = HBaseUtil.getTable("user_action")) {
            Scan scan = new Scan();
            scan.setRowPrefixFilter(userId.getBytes());
            System.out.println("开始扫描，前缀：" + userId);
            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    if (result.isEmpty()) continue;
                    String row = new String(result.getRow());
                    System.out.println("扫描到行键：" + row);
                    Map<String, Object> map = new HashMap<>();
                    byte[] urlBytes = result.getValue("info".getBytes(), "url".getBytes());
                    byte[] tsBytes = result.getValue("info".getBytes(), "ts".getBytes());
                    if (urlBytes != null && tsBytes != null) {
                        map.put("url", new String(urlBytes));
                        map.put("ts", new String(tsBytes));
                        list.add(map);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("查询结果条数：" + list.size());
        return list;
    }

}
