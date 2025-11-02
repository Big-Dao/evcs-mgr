package com.evcs.payment.service.message.impl;

import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.service.message.PaymentMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * 支付消息服务实现
 *
 * 目前为日志记录版本，后续可扩展为真正的消息队列实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMessageServiceImpl implements PaymentMessageService {

    @Value("${evcs.payment.message.enabled:false}")
    private boolean messageEnabled;

    @Override
    public void sendPaymentSuccessMessage(PaymentOrder paymentOrder) {
        if (!messageEnabled) {
            log.info("消息发送未启用，记录支付成功日志: orderId={}, tradeNo={}, amount={}",
                    paymentOrder.getOrderId(), paymentOrder.getTradeNo(), paymentOrder.getAmount());
            return;
        }

        try {
            // 构建支付成功消息
            String message = buildPaymentSuccessMessage(paymentOrder);

            // TODO: 实现RabbitMQ消息发送
            // rabbitTemplate.convertAndSend("payment.success.exchange", "payment.success.routing.key", message);

            log.info("支付成功消息已发送: {}", message);

        } catch (Exception e) {
            log.error("发送支付成功消息失败: orderId={}", paymentOrder.getOrderId(), e);
            // 不影响主流程，只记录错误
        }
    }

    @Override
    public void sendPaymentFailureMessage(PaymentOrder paymentOrder) {
        if (!messageEnabled) {
            log.info("消息发送未启用，记录支付失败日志: orderId={}, tradeNo={}, amount={}",
                    paymentOrder.getOrderId(), paymentOrder.getTradeNo(), paymentOrder.getAmount());
            return;
        }

        try {
            String message = buildPaymentFailureMessage(paymentOrder);

            // TODO: 实现RabbitMQ消息发送
            // rabbitTemplate.convertAndSend("payment.failure.exchange", "payment.failure.routing.key", message);

            log.info("支付失败消息已发送: {}", message);

        } catch (Exception e) {
            log.error("发送支付失败消息失败: orderId={}", paymentOrder.getOrderId(), e);
        }
    }

    @Override
    public void sendRefundSuccessMessage(PaymentOrder paymentOrder) {
        if (!messageEnabled) {
            log.info("消息发送未启用，记录退款成功日志: orderId={}, tradeNo={}, refundAmount={}",
                    paymentOrder.getOrderId(), paymentOrder.getTradeNo(), paymentOrder.getRefundAmount());
            return;
        }

        try {
            String message = buildRefundSuccessMessage(paymentOrder);

            // TODO: 实现RabbitMQ消息发送
            // rabbitTemplate.convertAndSend("refund.success.exchange", "refund.success.routing.key", message);

            log.info("退款成功消息已发送: {}", message);

        } catch (Exception e) {
            log.error("发送退款成功消息失败: orderId={}", paymentOrder.getOrderId(), e);
        }
    }

    /**
     * 构建支付成功消息
     */
    private String buildPaymentSuccessMessage(PaymentOrder paymentOrder) {
        return String.format("支付成功 - 订单ID: %s, 交易号: %s, 金额: %.2f, 支付方式: %s, 支付时间: %s",
                paymentOrder.getOrderId(),
                paymentOrder.getTradeNo(),
                paymentOrder.getAmount(),
                paymentOrder.getPaymentMethod(),
                paymentOrder.getPaidTime() != null ?
                    paymentOrder.getPaidTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "未知");
    }

    /**
     * 构建支付失败消息
     */
    private String buildPaymentFailureMessage(PaymentOrder paymentOrder) {
        return String.format("支付失败 - 订单ID: %s, 交易号: %s, 金额: %.2f, 支付方式: %s",
                paymentOrder.getOrderId(),
                paymentOrder.getTradeNo(),
                paymentOrder.getAmount(),
                paymentOrder.getPaymentMethod());
    }

    /**
     * 构建退款成功消息
     */
    private String buildRefundSuccessMessage(PaymentOrder paymentOrder) {
        return String.format("退款成功 - 订单ID: %s, 交易号: %s, 退款金额: %.2f, 退款时间: %s",
                paymentOrder.getOrderId(),
                paymentOrder.getTradeNo(),
                paymentOrder.getRefundAmount(),
                paymentOrder.getRefundTime() != null ?
                    paymentOrder.getRefundTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "未知");
    }
}