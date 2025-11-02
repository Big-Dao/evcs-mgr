package com.evcs.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

/**
 * 支付服务测试应用程序
 * 排除RabbitMQ自动配置和监听器
 */
@SpringBootApplication(exclude = {
    RabbitAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
@ComponentScan(excludeFilters = @ComponentScan.Filter(
    type = FilterType.REGEX,
    pattern = {
        "com\\.evcs\\.payment\\.service\\.listener\\..*",
        "com\\.evcs\\.payment\\.config\\.RabbitMQConfig.*"
    }
))
@Profile("test")
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}