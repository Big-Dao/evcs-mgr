package com.evcs.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 支付服务启动类
 */
@SpringBootApplication(
    scanBasePackages = {"com.evcs.payment", "com.evcs.common"},
    exclude = {SecurityAutoConfiguration.class}
)
@EnableDiscoveryClient
@MapperScan("com.evcs.payment.mapper")
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
