package com.example.demo.projects.realrank;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:47:34
 * @Version 1.0
 */
@Controller
@RequestMapping("/score")
public class ScoreController {
    @Autowired
    private ScoreProducerService producerService;
    @Autowired
    private ScoreConsumerService consumerService;

    @GetMapping("/demo")
    public String demo() { return "score_demo"; }

    @PostMapping("/produce")
    @ResponseBody
    public String produce(@RequestParam String userId, @RequestParam int score) {
        producerService.sendScore(userId, score, System.currentTimeMillis());
        return "OK";
    }

    @PostMapping("/batch")
    @ResponseBody
    public String batch() {
        producerService.batchSend();
        return "OK";
    }

    @GetMapping("/rank")
    @ResponseBody
    public List<UserScoreDTO> rank(@RequestParam(defaultValue = "10") int n) {
        return consumerService.getTopN(n);
    }

    @GetMapping("/profile")
    @ResponseBody
    public Map<String, Object> profile(@RequestParam String userId) {
        return consumerService.getUserProfile(userId);
    }

    @GetMapping("/timeseries")
    @ResponseBody
    public Map<String, Long> timeseries(@RequestParam String userId) {
        return consumerService.getUserTimeSeries(userId);
    }
}
