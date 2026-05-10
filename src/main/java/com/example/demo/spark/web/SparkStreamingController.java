package com.example.demo.spark.web;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.spark.sql.functions.*;

/**
 * Spark Structured Streaming 消费 Kafka，做实时单词计数
 *
 * GET  /spark/streaming         → 模板
 * POST /spark/streaming/start   → 启动 streaming query（如已启动则忽略）
 * POST /spark/streaming/stop    → 停止
 * POST /spark/streaming/produce → 往 Kafka topic spark-input 发一行文本
 *
 * 要点：
 *   - 用 readStream + writeStream + foreachBatch 写出"流批一体"的 API
 *   - micro-batch 默认 200ms，foreachBatch 里再把累计计数推到 WebSocket
 *   - 演示前先起 Kafka：docker compose -f docker/kafka/docker-compose.yml up -d
 *     并执行 docker/kafka/init-topics.sh（spark-input 会自动建）
 */
@Controller
@RequestMapping("/spark/streaming")
public class SparkStreamingController {

    private static final String TOPIC = "spark-input";

    @Autowired private SparkSession spark;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private SimpMessagingTemplate ws;

    /** 累计计数：所有 micro-batch 的 word -> count 汇总 */
    private final Map<String, AtomicLong> totals = new ConcurrentHashMap<>();
    private volatile StreamingQuery query;

    @GetMapping
    public String demo() {
        return "spark/streaming_demo";
    }

    @PostMapping("/produce")
    @ResponseBody
    public String produce(@RequestParam String text) {
        kafkaTemplate.send(TOPIC, text);
        return "OK";
    }

    @GetMapping("/totals")
    @ResponseBody
    public Map<String, Long> totals() {
        Map<String, Long> snap = new TreeMap<>();
        totals.forEach((k, v) -> snap.put(k, v.get()));
        return snap;
    }

    @PostMapping("/start")
    @ResponseBody
    public synchronized String start() {
        if (query != null && query.isActive()) return "already running";
        totals.clear();

        Dataset<Row> kafkaSource = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "localhost:9092")
                .option("subscribe", TOPIC)
                .option("startingOffsets", "latest")
                .load();

        Dataset<Row> words = kafkaSource
                .selectExpr("CAST(value AS STRING) as line")
                .selectExpr("explode(split(lower(line), '\\\\W+')) as word")
                .filter("length(word) > 0")
                .groupBy("word")
                .count();

        try {
            query = words.writeStream()
                    .outputMode("complete")
                    .trigger(Trigger.ProcessingTime("500 milliseconds"))
                    .foreachBatch((Dataset<Row> batch, Long batchId) -> {
                        // 流式聚合的 micro-batch 是"全量"结果，直接覆盖累计表
                        totals.clear();
                        for (Row r : batch.collectAsList()) {
                            totals.computeIfAbsent(r.getString(0), k -> new AtomicLong())
                                  .set(r.getLong(1));
                        }
                        ws.convertAndSend("/topic/spark-stream", totals());
                    })
                    .start();
            return "started, queryId=" + query.id();
        } catch (Exception e) {
            return "ERR: " + e.getMessage();
        }
    }

    @PostMapping("/stop")
    @ResponseBody
    public synchronized String stop() {
        if (query == null || !query.isActive()) return "not running";
        try {
            query.stop();
            return "stopped";
        } catch (Exception e) {
            return "ERR: " + e.getMessage();
        }
    }
}
