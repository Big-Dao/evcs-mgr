package com.evcs.protocol;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试应用程序入口
 * 用于集成测试
 */
@SpringBootApplication
public class TestProtocolApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestProtocolApplication.class, args);
    }
}
