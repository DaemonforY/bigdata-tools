package com.example.demo.projects.realrank;

import com.example.demo.hbase.HBaseUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

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
    // 实时排行榜TopN
    public List<Map<String, Object>> getTopN(int n) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            List<String> users = jedis.zrevrange("score:rank", 0, n - 1);
            for (String user : users) {
                Double score = jedis.zscore("score:rank", user);
                Map<String, Object> map = new HashMap<>();
                map.put("userId", user);
                map.put("score", score == null ? 0 : score.intValue());
                list.add(map);
            }
        }
        return list;
    }

    // 某用户总分
    public int getUserTotalScore(String userId) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            Double score = jedis.zscore("score:rank", userId);
            return score == null ? 0 : score.intValue();
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
