package com.example.demo.spark.web;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import scala.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spark RDD WordCount —— Web 版
 *
 * GET  /spark/wordcount       → 模板
 * POST /spark/wordcount/run   → 提交一段文本，立即返回单词频次
 *
 * 学生看点：同一个 SparkSession 反复执行 collect()，体验"懒执行 + 触发"
 */
@Controller
@RequestMapping("/spark/wordcount")
public class SparkWordCountController {

    @Autowired
    private SparkSession spark;

    @GetMapping
    public String demo() {
        return "spark/wordcount_demo";
    }

    @PostMapping("/run")
    @ResponseBody
    public Map<String, Object> run(@RequestParam String text) {
        long t0 = System.currentTimeMillis();
        JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());

        // 把每一行作为一条记录
        List<String> lines = Arrays.stream(text.split("\\R"))
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        JavaRDD<String> linesRdd = sc.parallelize(lines);
        JavaPairRDD<String, Integer> counts = linesRdd
                .flatMap(l -> Arrays.asList(l.toLowerCase().split("\\W+")).iterator())
                .filter(w -> !w.isBlank())
                .mapToPair(w -> new Tuple2<>(w, 1))
                .reduceByKey(Integer::sum);

        Map<String, Long> result = new TreeMap<>();
        counts.collect().forEach(t -> result.put(t._1, t._2.longValue()));

        return Map.of(
                "elapsedMs", System.currentTimeMillis() - t0,
                "uniqueWords", result.size(),
                "counts", result
        );
    }
}
