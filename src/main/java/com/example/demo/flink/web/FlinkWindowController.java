package com.example.demo.flink.web;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Flink Tumbling Event-Time Window + Watermark 演示
 *
 * 场景：用户点击事件流，按 5 秒滚动窗口统计每个用户的点击数
 *
 * 流程：
 *   - 浏览器点按钮 → POST /flink/window/click 把事件塞进队列
 *   - 后台 Flink Job 从队列源源不断取事件 → 提取事件时间 → keyBy(user) → 5s 滚动窗口 → 计数
 *   - 窗口触发时通过 WebSocket 把 (window, user, count) 推回页面
 *
 * 学生看点：
 *   1. WatermarkStrategy.forBoundedOutOfOrderness：允许 1s 乱序的水位线
 *   2. 滚动窗口（TumblingEventTimeWindows.of(5s)）：到点即关，不会等迟到太久
 *   3. ProcessWindowFunction：窗口算子里能看到 window 元数据（start/end）
 */
@Controller
@RequestMapping("/flink/window")
public class FlinkWindowController {

    /** 全局事件队列，作为 Flink 自定义 source */
    private static final BlockingQueue<ClickEvent> QUEUE = new LinkedBlockingQueue<>();
    private final SimpMessagingTemplate ws;
    private volatile Thread jobThread;

    public FlinkWindowController(SimpMessagingTemplate ws) {
        this.ws = ws;
    }

    @GetMapping
    public String demo() {
        return "flink/window_demo";
    }

    @PostMapping("/click")
    @ResponseBody
    public String click(@RequestParam String user) {
        QUEUE.offer(new ClickEvent(user, Instant.now().toEpochMilli()));
        return "OK";
    }

    @PostMapping("/start")
    @ResponseBody
    public synchronized String start() {
        if (jobThread != null && jobThread.isAlive()) return "already running";
        jobThread = new Thread(this::runJob, "flink-window-job");
        jobThread.setDaemon(true);
        jobThread.start();
        return "started";
    }

    private void runJob() {
        try {
            StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment(1);
            env.setParallelism(1);

            DataStream<ClickEvent> source = env
                    .addSource(new QueueSource())
                    .returns(Types.GENERIC(ClickEvent.class));

            WatermarkStrategy<ClickEvent> wm = WatermarkStrategy
                    .<ClickEvent>forBoundedOutOfOrderness(Duration.ofSeconds(1))
                    .withTimestampAssigner((e, ts) -> e.eventTime);

            DataStream<Tuple3<Long, String, Long>> agg = source
                    .assignTimestampsAndWatermarks(wm)
                    .keyBy(e -> e.user)
                    .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                    .process(new CountByWindow());

            // 推到 WebSocket
            agg.addSink(new org.apache.flink.streaming.api.functions.sink.SinkFunction<>() {
                @Override
                public void invoke(Tuple3<Long, String, Long> v, Context ctx) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("windowEnd", v.f0);
                    m.put("user", v.f1);
                    m.put("count", v.f2);
                    ws.convertAndSend("/topic/flink-window", m);
                }
            });

            env.execute("flink-window-demo");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ClickEvent {
        public String user;
        public long eventTime;
        public ClickEvent() {}
        public ClickEvent(String user, long ts) { this.user = user; this.eventTime = ts; }
    }

    /** 把 BlockingQueue 包成无限 source；停止时丢一个毒丸（user=null） */
    public static class QueueSource implements org.apache.flink.streaming.api.functions.source.SourceFunction<ClickEvent> {
        private volatile boolean running = true;
        @Override
        public void run(SourceContext<ClickEvent> ctx) throws Exception {
            while (running) {
                ClickEvent e = QUEUE.poll(500, TimeUnit.MILLISECONDS);
                if (e == null) continue;
                if (e.user == null) break;
                ctx.collect(e);
            }
        }
        @Override
        public void cancel() { running = false; }
    }

    /** 窗口聚合：输出 (windowEnd, user, count) */
    public static class CountByWindow extends ProcessWindowFunction<ClickEvent, Tuple3<Long, String, Long>, String, TimeWindow> {
        @Override
        public void process(String user, Context ctx,
                            Iterable<ClickEvent> elements,
                            Collector<Tuple3<Long, String, Long>> out) {
            long c = 0;
            for (ClickEvent ignored : elements) c++;
            out.collect(Tuple3.of(ctx.window().getEnd(), user, c));
        }
    }
}
