package com.example.demo.flink.web;

import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * Flink CEP（Complex Event Processing）演示：
 * 同一个用户在 30 秒内连续 3 次登录失败，触发风控告警。
 *
 * 学生看点：
 *   1. CEP Pattern API：begin / next / times / within，把"复杂事件"声明出来
 *   2. CEP 内部其实是 NFA（非确定有限自动机），只跟踪可能匹配的状态前缀
 *   3. 与 Spark 的 micro-batch 相比，Flink 真的能"边来边判"，延迟极低
 *
 * 用法：
 *   1. /flink/cep/start 启动 Job
 *   2. /flink/cep/login?user=alice&result=FAIL 多次 → 等 alert
 */
@Controller
@RequestMapping("/flink/cep")
public class FlinkCepController {

    private static final BlockingQueue<LoginEvent> QUEUE = new LinkedBlockingQueue<>();
    private final SimpMessagingTemplate ws;
    private volatile Thread jobThread;

    public FlinkCepController(SimpMessagingTemplate ws) {
        this.ws = ws;
    }

    @GetMapping
    public String demo() {
        return "flink/cep_demo";
    }

    @PostMapping("/login")
    @ResponseBody
    public String login(@RequestParam String user, @RequestParam String result) {
        QUEUE.offer(new LoginEvent(user, result.toUpperCase(), Instant.now().toEpochMilli()));
        return "OK";
    }

    @PostMapping("/start")
    @ResponseBody
    public synchronized String start() {
        if (jobThread != null && jobThread.isAlive()) return "already running";
        jobThread = new Thread(this::runJob, "flink-cep-job");
        jobThread.setDaemon(true);
        jobThread.start();
        return "started";
    }

    private void runJob() {
        try {
            StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironment(1);
            env.setParallelism(1);

            DataStream<LoginEvent> source = env.addSource(new QueueSource())
                    .returns(Types.GENERIC(LoginEvent.class));

            // Pattern：连续 3 次 FAIL，30s 内
            Pattern<LoginEvent, ?> pattern = Pattern.<LoginEvent>begin("first")
                    .where(new SimpleCondition<>() {
                        @Override public boolean filter(LoginEvent e) { return "FAIL".equals(e.result); }
                    })
                    .next("second").where(new SimpleCondition<>() {
                        @Override public boolean filter(LoginEvent e) { return "FAIL".equals(e.result); }
                    })
                    .next("third").where(new SimpleCondition<>() {
                        @Override public boolean filter(LoginEvent e) { return "FAIL".equals(e.result); }
                    })
                    .within(Time.seconds(30));

            PatternStream<LoginEvent> ps = CEP.pattern(source.keyBy(e -> e.user), pattern);

            DataStream<Map<String, Object>> alerts = ps.select(events -> {
                LoginEvent first  = events.get("first").get(0);
                LoginEvent third  = events.get("third").get(0);
                Map<String, Object> alert = new LinkedHashMap<>();
                alert.put("user", first.user);
                alert.put("firstTs", first.ts);
                alert.put("thirdTs", third.ts);
                alert.put("spanMs", third.ts - first.ts);
                alert.put("alert", "BRUTE_FORCE_LOGIN");
                return alert;
            });

            alerts.addSink(new org.apache.flink.streaming.api.functions.sink.SinkFunction<>() {
                @Override
                public void invoke(Map<String, Object> v, Context ctx) {
                    ws.convertAndSend("/topic/flink-cep", v);
                }
            });

            env.execute("flink-cep-demo");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static class LoginEvent {
        public String user;
        public String result;   // SUCCESS / FAIL
        public long ts;
        public LoginEvent() {}
        public LoginEvent(String user, String result, long ts) { this.user = user; this.result = result; this.ts = ts; }
    }

    public static class QueueSource implements SourceFunction<LoginEvent> {
        private volatile boolean running = true;
        @Override
        public void run(SourceContext<LoginEvent> ctx) throws Exception {
            while (running) {
                LoginEvent e = QUEUE.poll(500, TimeUnit.MILLISECONDS);
                if (e == null) continue;
                if (e.user == null) break;
                ctx.collect(e);
            }
        }
        @Override public void cancel() { running = false; }
    }
}
