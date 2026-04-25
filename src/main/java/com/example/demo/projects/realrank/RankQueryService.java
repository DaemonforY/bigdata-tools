package com.example.demo.projects.realrank;

import com.example.demo.hbase.HBaseUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:48:39
 * @Version 1.0
 */

@Service
public class RankQueryService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 实时排行榜TopN
    public List<Map<String, Object>> getTopN(int n) {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Set<String> users = stringRedisTemplate.opsForZSet().reverseRange("score:rank", 0, n - 1);
            if (users == null) {
                return list;
            }
            for (String user : users) {
                Double score = stringRedisTemplate.opsForZSet().score("score:rank", user);
                Map<String, Object> map = new HashMap<>();
                map.put("userId", user);
                map.put("score", score == null ? 0 : score.intValue());
                list.add(map);
            }
        } catch (DataAccessException e) {
            System.err.println("Redis 不可用，无法查询排行榜: " + e.getMessage());
        }
        return list;
    }

    // 某用户总分
    public int getUserTotalScore(String userId) {
        try {
            Double score = stringRedisTemplate.opsForZSet().score("score:rank", userId);
            return score == null ? 0 : score.intValue();
        } catch (DataAccessException e) {
            System.err.println("Redis 不可用，无法查询用户总分: " + e.getMessage());
            return 0;
        }
    }

    // 历史得分
    public List<Map<String, Object>> getUserHistory(String userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Table table = HBaseUtil.getTable("score_history")) {
            Scan scan = new Scan();
            scan.setRowPrefixFilter(userId.getBytes(StandardCharsets.UTF_8));
            try (ResultScanner scanner = table.getScanner(scan)) {
                for (Result result : scanner) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("rowKey", new String(result.getRow()));
                    map.put("score", new String(result.getValue("info".getBytes(), "score".getBytes())));
                    map.put("ts", new String(result.getValue("info".getBytes(), "ts".getBytes())));
                    list.add(map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 按时间降序
        list.sort((a, b) -> ((String)b.get("ts")).compareTo((String)a.get("ts")));
        return list;
    }
}
