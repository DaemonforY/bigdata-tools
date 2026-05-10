package com.example.demo.spark.demo;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.util.Arrays;
import java.util.List;

/**
 * Spark 入门第 0 课：RDD WordCount
 *
 * 学习目标：
 *   1. 理解 RDD 的"懒执行"——transformations(map/flatMap/...) 不会立即执行，
 *      直到调用 actions(collect/count/...) 才触发真实计算
 *   2. 看懂 reduceByKey 的 "combine then shuffle" 优化
 *   3. 知道为什么相同的代码可以在 1 台机器和 1000 台机器上跑
 *
 * 运行方式（任选一种）：
 *   A) IDE 直接 run main()，控制台看输出
 *   B) mvn package 后：
 *      docker exec spark-master /opt/bitnami/spark/bin/spark-submit \
 *        --master spark://spark-master:7077 \
 *        --class com.example.demo.spark.demo.RddWordCount \
 *        /opt/jobs/demo.jar
 */
public class RddWordCount {

    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
                .appName("rdd-wordcount")
                .master("local[*]")
                .config("spark.driver.bindAddress", "127.0.0.1")
                .getOrCreate();

        JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());
        sc.setLogLevel("WARN");

        // 1) 把内存里的字符串集合并行化成 RDD（实际工作中常用 sc.textFile("hdfs://...")）
        List<String> lines = Arrays.asList(
                "hello spark",
                "hello flink",
                "hello kafka and spark",
                "spark is fast"
        );
        JavaRDD<String> linesRdd = sc.parallelize(lines, 2);

        // 2) flatMap：一行 -> 多个单词（注意 flatMap 与 map 的区别：1->N vs 1->1）
        JavaRDD<String> wordsRdd = linesRdd.flatMap(line -> Arrays.asList(line.split("\\s+")).iterator());

        // 3) mapToPair + reduceByKey：经典三步走
        //    reduceByKey 会在 shuffle 前先做一次本地预聚合（combiner），比 groupByKey 快得多
        JavaPairRDD<String, Integer> counts = wordsRdd
                .mapToPair(w -> new Tuple2<>(w, 1))
                .reduceByKey(Integer::sum);

        // 4) action：触发真实计算并把结果拉回 Driver
        counts.collect().forEach(t -> System.out.println(t._1 + " -> " + t._2));

        spark.stop();
    }
}
