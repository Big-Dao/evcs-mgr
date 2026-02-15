package com.evcs.eureka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = EurekaServerApplication.class,
    properties = {
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false"
    }
)
@DisplayName("Eureka服务启动测试")
class EurekaServerApplicationTest {

    @Test
    @DisplayName("应用上下文 - 应成功加载")
    void testContextLoads() {
    }
}
