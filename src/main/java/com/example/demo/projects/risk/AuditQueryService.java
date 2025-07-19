package com.example.demo.projects.risk;

import com.example.demo.hbase.HBaseUtil;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:39:05
 * @Version 1.0
 */

@Service
public class AuditQueryService {
    public Map<String, String> getOrderAudit(String orderId) {
        Map<String, String> map = new HashMap<>();
        try (Table table = HBaseUtil.getTable("order_audit")) {
            Get get = new Get(orderId.getBytes());
            Result result = table.get(get);
            byte[] status = result.getValue("info".getBytes(), "status".getBytes());
            byte[] detail = result.getValue("info".getBytes(), "detail".getBytes());
            map.put("status", status == null ? "无" : new String(status));
            map.put("detail", detail == null ? "无" : new String(detail));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}

