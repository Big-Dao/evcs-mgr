package com.evcs.payment.config;

import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.dto.RefundRequest;
import com.evcs.payment.dto.RefundResponse;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.service.channel.IPaymentChannel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.math.BigDecimal;

/**
 * Mock支付渠道配置 - 用于测试环境
 */
@TestConfiguration
public class MockPaymentChannelConfig {

    @Bean
    @Primary
    public IPaymentChannel mockAlipayChannel() {
        return new MockPaymentChannel("alipay");
    }

    @Bean
    @Primary
    public IPaymentChannel mockWechatChannel() {
        return new MockPaymentChannel("wechat");
    }

    /**
     * Mock支付渠道实现
     */
    public static class MockPaymentChannel implements IPaymentChannel {
        private final String channelType;

        public MockPaymentChannel(String channelType) {
            this.channelType = channelType;
        }

        @Override
        public PaymentResponse createPayment(PaymentRequest request) {
            PaymentResponse response = new PaymentResponse();
            response.setPaymentId(System.currentTimeMillis());
            response.setTradeNo(generateTradeNo());
            response.setStatus(PaymentStatus.PENDING);
            response.setAmount(request.getAmount());

            // 根据支付方式设置不同的响应参数
            if (request.getPaymentMethod().name().contains("QR") || request.getPaymentMethod().name().contains("NATIVE")) {
                response.setPayUrl("https://mock-" + channelType + ".com/qr/" + response.getTradeNo());
            } else {
                response.setPayParams("{\"order_string\":\"mock_order_string\"}");
            }

            return response;
        }

        @Override
        public boolean verifySignature(String data, String signature) {
            // Mock签名验证总是成功
            return true;
        }

        @Override
        public PaymentResponse queryPayment(String tradeNo) {
            PaymentResponse response = new PaymentResponse();
            response.setTradeNo(tradeNo);
            response.setStatus(PaymentStatus.SUCCESS);
            response.setAmount(new BigDecimal("100.00"));
            return response;
        }

        @Override
        public RefundResponse refund(RefundRequest request) {
            RefundResponse response = new RefundResponse();
            response.setRefundNo("RF" + System.currentTimeMillis());
            response.setRefundAmount(request.getRefundAmount());
            response.setRefundStatus("SUCCESS");
            return response;
        }

        @Override
        public String getChannelName() {
            return channelType;
        }

        private String generateTradeNo() {
            return channelType.toUpperCase().substring(0, 3) +
                   System.currentTimeMillis() +
                   String.format("%06d", (int)(Math.random() * 1000000));
        }
    }
}