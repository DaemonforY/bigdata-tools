package com.example.demo.danmu;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/danmus")
public class DanmuController {

    private static final int MAX_DANMU_COUNT = 100;

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Deque<Map<String, Object>> danmus = new ConcurrentLinkedDeque<>();

    @GetMapping("/list")
    public List<Map<String, Object>> list() {
        return new ArrayList<>(danmus);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, String> body) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("content 不能为空");
        }

        Map<String, Object> danmu = Map.of(
                "id", idGenerator.getAndIncrement(),
                "content", content,
                "ts", System.currentTimeMillis()
        );

        danmus.addLast(danmu);
        while (danmus.size() > MAX_DANMU_COUNT) {
            danmus.pollFirst();
        }
        return danmu;
    }
}
