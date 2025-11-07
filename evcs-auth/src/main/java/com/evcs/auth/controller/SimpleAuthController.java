package com.evcs.auth.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

/**
 * 极简认证控制器 - 用于测试控制器映射
 */
@RestController
@RequestMapping("")
public class SimpleAuthController {

    /**
     * 测试端点
     */
    @GetMapping("/test")
    public String test() {
        return "SimpleAuthController is working!";
    }

    /**
     * 简化的登录端点
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> request) {
        return Map.of(
            "code", 200,
            "message", "临时登录成功",
            "data", "test-token"
        );
    }
}