package com.example.demo.flink.web;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Flink DataStream WordCount —— Web 版（每次请求起一个 mini-cluster 跑完即停）
 *
 * 这种用法只适合教学：
 *   - 优点：学生在浏览器就能看 Flink 是怎么把"程序变成执行图"的
 *   - 缺点：每次都重启 env，没有真实流的"持续算"语义
 *
 * 真实生产请走 spark-submit 或 flink run 提交到集群。
 */
@Controller
@RequestMapping("/flink/wordcount")
public class FlinkWordCountController {

    @GetMapping
    public String demo() {
        return "flink/wordcount_demo";
    }

    @PostMapping("/run")
    @ResponseBody
    public Map<String, Object> run(@RequestParam String text) throws Exception {
        long t0 = System.currentTimeMillis();

        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment(2);
        List<String> lines = Arrays.stream(text.split("\\R")).filter(s -> !s.isBlank()).collect(Collectors.toList());

        // 用列表来收集 sink 的输出（教学方便，生产用 Sink）
        List<Tuple2<String, Integer>> sink = Collections.synchronizedList(new ArrayList<>());

        DataStream<Tuple2<String, Integer>> counts = env
                .fromCollection(lines)
                .flatMap((FlatMapFunction<String, Tuple2<String, Integer>>) (line, out) -> {
                    for (String w : line.toLowerCase().split("\\W+")) {
                        if (!w.isBlank()) out.collect(Tuple2.of(w, 1));
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(t -> t.f0)
                .sum(1);

        // 用 executeAndCollect 收所有结果（仅 1.18+ 用法）
        try (var iter = counts.executeAndCollect()) {
            while (iter.hasNext()) sink.add(iter.next());
        }

        // 同一个 key 多次累加，最后一个值才是最终值 → 取 max
        Map<String, Integer> finalCounts = new TreeMap<>();
        sink.forEach(t -> finalCounts.merge(t.f0, t.f1, Math::max));

        return Map.of(
                "elapsedMs", System.currentTimeMillis() - t0,
                "uniqueWords", finalCounts.size(),
                "counts", finalCounts
        );
    }
}
