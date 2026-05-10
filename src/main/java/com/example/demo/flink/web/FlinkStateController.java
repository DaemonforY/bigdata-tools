package com.example.demo.flink.web;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.util.Collector;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Flink Keyed State + Timer 演示：用户会话超时检测
 *
 * 场景：用户每次操作（点击）都会刷新自己的"最后活跃时间"。
 *      若 10 秒内无任何操作，则认为会话已超时，输出超时事件。
 *
 * 学生看点：
 *   1. ValueState：每个 key 一份私有状态（这里存"最后一次事件时间戳"）
 *   2. processElement / onTimer：Flink 的"事件 + 定时器"双驱动模型
 *   3. registerProcessingTimeTimer：处理时间定时器，到点就触发
 *   4. 状态更新和定时器是 exactly-once 的（开启 checkpoint 后）
 */
@Controller
@RequestMapping("/flink/state")
public class FlinkStateController {

    private static final BlockingQueue<Event> QUEUE = new LinkedBlockingQueue<>();
    private final SimpMessagingTemplate ws;
    private volatile Thread jobThread;

    public FlinkStateController(SimpMessagingTemplate ws) {
        this.ws = ws;
    }

    @GetMapping
    public String demo() {
        return "flink/state_demo";
    }

    @PostMapping("/click")
    @ResponseBody
    public String click(@RequestParam String user) {
        QUEUE.offer(new Event(user, Instant.now().toEpochMilli()));
        return "OK";
    }

    @PostMapping("/start")
    @ResponseBody
    public synchronized String start() {
        if (jobThread != null && jobThread.isAlive()) return "already running";
        jobThread = new Thread(this::runJob, "flink-state-job");
        jobThread.setDaemon(true);
        jobThread.start();
        return "started";
    }

    private void runJob() {
        try {
            StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment(1);
            env.setParallelism(1);

            DataStream<Event> source = env.addSource(new QueueSource())
                    .returns(Types.GENERIC(Event.class));

            DataStream<Map<String, Object>> alerts = source
                    .keyBy(e -> e.user)
                    .process(new SessionTimeoutFn(10_000));

            alerts.addSink(new org.apache.flink.streaming.api.functions.sink.SinkFunction<>() {
                @Override
                public void invoke(Map<String, Object> v, Context ctx) {
                    ws.convertAndSend("/topic/flink-state", v);
                }
            });

            env.execute("flink-state-demo");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static class Event {
        public String user;
        public long ts;
        public Event() {}
        public Event(String user, long ts) { this.user = user; this.ts = ts; }
    }

    public static class QueueSource implements SourceFunction<Event> {
        private volatile boolean running = true;
        @Override
        public void run(SourceContext<Event> ctx) throws Exception {
            while (running) {
                Event e = QUEUE.poll(500, TimeUnit.MILLISECONDS);
                if (e == null) continue;
                if (e.user == null) break;
                ctx.collect(e);
            }
        }
        @Override public void cancel() { running = false; }
    }

    /**
     * 每来一个事件：刷新 lastSeen，并注册一个 timeoutMs 之后的 processing-time 定时器。
     * 定时器触发时检查 "现在 - lastSeen >= timeoutMs"，是则发超时事件 + 清状态。
     */
    public static class SessionTimeoutFn extends KeyedProcessFunction<String, Event, Map<String, Object>> {
        private final long timeoutMs;
        private transient ValueState<Long> lastSeen;

        public SessionTimeoutFn(long timeoutMs) { this.timeoutMs = timeoutMs; }

        @Override
        public void open(Configuration parameters) {
            lastSeen = getRuntimeContext().getState(
                    new ValueStateDescriptor<>("last-seen", Types.LONG));
        }

        @Override
        public void processElement(Event e, Context ctx, Collector<Map<String, Object>> out) throws Exception {
            lastSeen.update(e.ts);
            // 注册"timeout 后"的定时器；后到的事件会注册新的定时器，老的会自然失效（onTimer 里再判一次时间）
            ctx.timerService().registerProcessingTimeTimer(System.currentTimeMillis() + timeoutMs);

            Map<String, Object> alive = new LinkedHashMap<>();
            alive.put("type", "ACTIVE");
            alive.put("user", e.user);
            alive.put("ts", e.ts);
            out.collect(alive);
        }

        @Override
        public void onTimer(long timerTs, OnTimerContext ctx, Collector<Map<String, Object>> out) throws Exception {
            Long last = lastSeen.value();
            if (last == null) return;
            if (System.currentTimeMillis() - last >= timeoutMs) {
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("type", "TIMEOUT");
                alert.put("user", ctx.getCurrentKey());
                alert.put("idleMs", System.currentTimeMillis() - last);
                out.collect(alert);
                lastSeen.clear();
            }
        }
    }
}
