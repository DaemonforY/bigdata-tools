package com.example.demo.projects.realrank;

import com.example.demo.hbase.HBaseUtil;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:45:39
 * @Version 1.0
 */

@Service
public class ScoreConsumerService {
    @Autowired
    private SimpMessagingTemplate wsTemplate;

    // 内存排行榜和历史，仅用于WebSocket推送演示（正式应用可用Redis/HBase）
    private final Map<String, Long> totalScoreMap = new HashMap<>();
    private final Map<String, List<ScoreRecord>> userHistoryMap = new HashMap<>();

    @KafkaListener(topics = "score", groupId = "score-group")
    public void consume(String msg) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            JSONObject obj = new JSONObject(msg);
            String userId = obj.getString("userId");
            int score = obj.getInt("score");
            long ts = obj.getLong("ts");

            // 1. Redis ZSet排行榜
            jedis.zincrby("score:rank", score, userId);

            // 2. HBase历史得分
            Table table = HBaseUtil.getTable("score_history");
            String rowKey = userId + "_" + ts;
            Put put = new Put(rowKey.getBytes(StandardCharsets.UTF_8));
            put.addColumn("info".getBytes(), "score".getBytes(), String.valueOf(score).getBytes());
            put.addColumn("info".getBytes(), "ts".getBytes(), String.valueOf(ts).getBytes());
            table.put(put);
            table.close();

            // 3. 内存更新（用于WebSocket推送）
            totalScoreMap.put(userId, totalScoreMap.getOrDefault(userId, 0L) + score);
            userHistoryMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(new ScoreRecord(score, ts));

            // 4. WebSocket推送排行榜
            wsTemplate.convertAndSend("/topic/score/rank", getTopN(10));
            // 5. WebSocket推送用户画像
            wsTemplate.convertAndSend("/topic/score/profile/" + userId, getUserProfile(userId));
            // 6. WebSocket推送分时段
            wsTemplate.convertAndSend("/topic/score/timeseries/" + userId, getUserTimeSeries(userId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<UserScoreDTO> getTopN(int n) {
        return totalScoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .map(e -> new UserScoreDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public Map<String, Object> getUserProfile(String userId) {
        List<ScoreRecord> history = userHistoryMap.getOrDefault(userId, List.of());
        long total = history.stream().mapToLong(r -> r.score).sum();
        long max = history.stream().mapToLong(r -> r.score).max().orElse(0L);
        double avg = history.isEmpty() ? 0 : (double) total / history.size();
        return Map.of("total", total, "max", max, "avg", avg);
    }

    public Map<String, Long> getUserTimeSeries(String userId) {
        List<ScoreRecord> history = userHistoryMap.getOrDefault(userId, List.of());
        Map<String, Long> series = new TreeMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00").withZone(ZoneId.systemDefault());
        for (ScoreRecord r : history) {
            String hour = fmt.format(Instant.ofEpochMilli(r.ts));
            series.put(hour, series.getOrDefault(hour, 0L) + r.score);
        }
        return series;
    }

    record ScoreRecord(int score, long ts) {}
}
