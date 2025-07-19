package com.example.demo.projects.risk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Set;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:39:34
 * @Version 1.0
 */

@Controller
@RequestMapping("/risk")
public class RiskOrderController {
    @Autowired
    private BlacklistService blacklistService;
    @Autowired
    private RiskOrderProducerService producerService;
    @Autowired
    private AuditQueryService queryService;

    @GetMapping("/demo")
    public String demo() { return "risk_demo"; }

    // 黑名单管理
    @PostMapping("/blacklist/add")
    @ResponseBody
    public String addBlack(@RequestParam String userId) {
        blacklistService.add(userId);
        return "OK";
    }
    @PostMapping("/blacklist/remove")
    @ResponseBody
    public String removeBlack(@RequestParam String userId) {
        blacklistService.remove(userId);
        return "OK";
    }
    @GetMapping("/blacklist/list")
    @ResponseBody
    public Set<String> getBlack() { return blacklistService.getAll(); }

    // 订单生产
    @PostMapping("/produce")
    @ResponseBody
    public String produce(@RequestParam String orderId, @RequestParam String userId, @RequestParam double amount) {
        producerService.sendOrder(orderId, userId, amount);
        return "OK";
    }
    @PostMapping("/batch")
    @ResponseBody
    public String batch() {
        producerService.batchSend();
        return "OK";
    }

    // 查询订单审计
    @GetMapping("/audit")
    @ResponseBody
    public Map<String, String> getAudit(@RequestParam String orderId) {
        return queryService.getOrderAudit(orderId);
    }
}