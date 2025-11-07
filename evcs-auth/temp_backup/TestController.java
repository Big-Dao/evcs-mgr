package com.evcs.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 简单测试控制器
 */
@RestController
public class TestController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}