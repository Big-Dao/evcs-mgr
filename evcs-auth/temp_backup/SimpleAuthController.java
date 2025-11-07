package com.evcs.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 简化版认证控制器 - 不依赖外部类
 */
@RestController
@RequestMapping("")
public class SimpleAuthController {

    @GetMapping("/test")
    public String test() {
        return "SimpleAuthController is working!";
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        return Map.of(
            "code", 200,
            "message", "临时登录成功",
            "data", "test-token",
            "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "message", "认证服务运行正常"
        );
    }
}