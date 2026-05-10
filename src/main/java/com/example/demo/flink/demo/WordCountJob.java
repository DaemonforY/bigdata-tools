package com.example.demo.flink.demo;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * Flink 入门第 0 课：DataStream WordCount
 *
 * 学习目标：
 *   1. 理解 Flink 是"流优先"的：fromElements 给的是有界流，env.execute 才触发执行
 *   2. 看懂 keyBy + sum 的"按 key 累加"模式（这是 Flink 状态的最简单形态）
 *   3. 区分 Flink 与 Spark：Spark 是"批为主，流是 micro-batch"，Flink 是"流为主，批是有界流"
 *
 * 运行方式（任选一种）：
 *   A) IDE 直接 run main()，控制台看输出
 *   B) mvn package 后：
 *      docker cp target/demo-0.0.1-SNAPSHOT.jar flink-jobmanager:/opt/job.jar
 *      docker exec flink-jobmanager flink run \
 *        -c com.example.demo.flink.demo.WordCountJob /opt/job.jar
 *      然后到 http://localhost:8082 看 Job
 */
public class WordCountJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        DataStream<String> source = env.fromElements(
                "hello flink",
                "hello spark",
                "flink is real time",
                "kafka and flink"
        );

        DataStream<Tuple2<String, Integer>> counts = source
                .flatMap((FlatMapFunction<String, Tuple2<String, Integer>>) (line, out) -> {
                    for (String w : line.split("\\W+")) {
                        if (!w.isBlank()) out.collect(Tuple2.of(w.toLowerCase(), 1));
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(t -> t.f0)
                .sum(1);

        counts.print();
        env.execute("flink-wordcount");
    }
}
