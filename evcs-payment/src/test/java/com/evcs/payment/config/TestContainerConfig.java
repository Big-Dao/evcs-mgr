package com.evcs.payment.config;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 测试容器配置
 * 提供RabbitMQ测试容器，避免依赖外部RabbitMQ服务
 */
@TestConfiguration
@Testcontainers
@Profile("test")
public class TestContainerConfig {

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(
        DockerImageName.parse("rabbitmq:3.12-management-alpine"))
        .withExposedPorts(5672, 15672)
        .withReuse(true);

    @BeforeAll
    static void setup() {
        // 启动RabbitMQ容器
        rabbitmq.start();

        // 设置系统属性供测试配置使用
        System.setProperty("RABBITMQ_HOST", rabbitmq.getHost());
        System.setProperty("RABBITMQ_PORT", String.valueOf(rabbitmq.getMappedPort(5672)));
        System.setProperty("RABBITMQ_USERNAME", rabbitmq.getAdminUsername());
        System.setProperty("RABBITMQ_PASSWORD", rabbitmq.getAdminPassword());
    }

    /**
     * 获取RabbitMQ主机地址
     */
    @Bean
    public String rabbitmqHost() {
        return rabbitmq.getHost();
    }

    /**
     * 获取RabbitMQ端口
     */
    @Bean
    public Integer rabbitmqPort() {
        return rabbitmq.getMappedPort(5672);
    }

    /**
     * 获取RabbitMQ用户名
     */
    @Bean
    public String rabbitmqUsername() {
        return rabbitmq.getAdminUsername();
    }

    /**
     * 获取RabbitMQ密码
     */
    @Bean
    public String rabbitmqPassword() {
        return rabbitmq.getAdminPassword();
    }
}