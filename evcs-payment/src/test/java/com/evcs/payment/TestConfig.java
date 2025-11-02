package com.evcs.payment;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

/**
 * 支付服务测试配置
 * 提供测试环境所需的Bean
 */
@TestConfiguration
public class TestConfig {

    /**
     * 提供简单的MeterRegistry用于测试
     */
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * 提供模拟的支付消息服务用于测试
     */
    @Bean
    @Primary
    public com.evcs.payment.service.message.PaymentMessageService paymentMessageService() {
        return new com.evcs.payment.service.message.PaymentMessageService() {
            @Override
            public void sendPaymentSuccessMessage(com.evcs.payment.entity.PaymentOrder paymentOrder) {
                // 测试环境下只记录日志，不发送真实消息
                System.out.println("测试环境 - 支付成功消息: " + paymentOrder.getOrderId());
            }

            @Override
            public void sendPaymentFailureMessage(com.evcs.payment.entity.PaymentOrder paymentOrder) {
                System.out.println("测试环境 - 支付失败消息: " + paymentOrder.getOrderId());
            }

            @Override
            public void sendRefundSuccessMessage(com.evcs.payment.entity.PaymentOrder paymentOrder) {
                System.out.println("测试环境 - 退款成功消息: " + paymentOrder.getOrderId());
            }
        };
    }
}