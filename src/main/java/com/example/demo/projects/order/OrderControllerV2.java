package com.example.demo.projects.order;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/4 07:19:17
 * @Version 1.0
 */

@Controller
@RequestMapping("/order")
public class OrderControllerV2 {
    @Autowired
    private OrderProducerServiceV2 producerService;
    @Autowired
    private OrderQueryService queryService;

    @GetMapping("/demo")
    public String demo() {
        return "order_demo";
    }

    @PostMapping("/produce")
    @ResponseBody
    public String produce(@RequestParam String orderId, @RequestParam String userId, @RequestParam double amount) {
        producerService.sendOrder(orderId, userId, amount);
        return "OK";
    }

    @PostMapping("/changestatus")
    @ResponseBody
    public String changeStatus(@RequestParam String orderId, @RequestParam String status) {
        producerService.changeOrderStatus(orderId, status);
        return "OK";
    }

    @GetMapping("/redis")
    @ResponseBody
    public String getRedis(@RequestParam String orderId) {
        return queryService.getOrderStatusFromRedis(orderId);
    }

    @GetMapping("/hbase")
    @ResponseBody
    public Map<String, Object> getHBase(@RequestParam String orderId) {
        return queryService.getOrderStatusFromHBase(orderId);
    }
}
