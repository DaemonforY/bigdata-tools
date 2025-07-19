package com.example.demo.projects.useraction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 06:57:55
 * @Version 1.0
 */

@Controller
@RequestMapping("/useraction")
public class UserActionController {
    @Autowired
    private UserActionProducerService producerService;
    @Autowired
    private UserActionQueryService queryService;

    @GetMapping("/demo")
    public String demo() {
        return "useraction_demo";
    }

    @PostMapping("/produce")
    @ResponseBody
    public String produce(@RequestParam String userId, @RequestParam String url) {
        producerService.sendUserAction(userId, url, System.currentTimeMillis());
        return "OK";
    }

    @GetMapping("/redis")
    @ResponseBody
    public long getRedis(@RequestParam String userId) {
        return queryService.getRedisCount(userId);
    }

    @GetMapping("/hbase")
    @ResponseBody
    public List<Map<String, Object>> getHBase(@RequestParam String userId) {
        return queryService.getUserActions(userId);
    }
}
