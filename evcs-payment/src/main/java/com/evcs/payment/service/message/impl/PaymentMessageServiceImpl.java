package com.evcs.payment.service.message.impl;

import com.evcs.payment.config.RabbitMQConfig;
import com.evcs.payment.dto.message.PaymentMessage;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.service.message.PaymentMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付消息服务实现
 *
 * 支持RabbitMQ消息发送和降级处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentMessageServiceImpl implements PaymentMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig rabbitMQConfig;

    @Value("${evcs.payment.message.enabled:false}")
    private boolean messageEnabled;

    @Value("${evcs.payment.rabbitmq.enabled:false}")
    private boolean rabbitmqEnabled;

    @Override
    public void sendPaymentSuccessMessage(PaymentOrder paymentOrder) {
        if (!messageEnabled) {
            log.info("消息发送未启用，记录支付成功日志: orderId={}, tradeNo={}, amount={}",
                    paymentOrder.getOrderId(), paymentOrder.getTradeNo(), paymentOrder.getAmount());
            return;
        }

        try {
            PaymentMessage message = buildPaymentSuccessMessage(paymentOrder);

            if (rabbitmqEnabled) {
                // 发送RabbitMQ消息
                rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getExchange().getPaymentDirect(),
                    rabbitMQConfig.getRoutingKey().getPaymentSuccess(),
                    message
                );
                log.info("支付成功消息已发送到RabbitMQ: messageId={}, orderId={}, tradeNo={}",
                    message.getMessageId(), paymentOrder.getOrderId(), paymentOrder.getTradeNo());
            } else {
                // 降级为日志记录
                log.info("RabbitMQ未启用，记录支付成功日志: {}", message.toString());
            }

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
            PaymentMessage message = buildPaymentFailureMessage(paymentOrder);

            if (rabbitmqEnabled) {
                // 发送RabbitMQ消息
                rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getExchange().getPaymentDirect(),
                    rabbitMQConfig.getRoutingKey().getPaymentFailure(),
                    message
                );
                log.info("支付失败消息已发送到RabbitMQ: messageId={}, orderId={}, tradeNo={}",
                    message.getMessageId(), paymentOrder.getOrderId(), paymentOrder.getTradeNo());
            } else {
                // 降级为日志记录
                log.info("RabbitMQ未启用，记录支付失败日志: {}", message.toString());
            }

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
            PaymentMessage message = buildRefundSuccessMessage(paymentOrder);

            if (rabbitmqEnabled) {
                // 发送RabbitMQ消息
                rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getExchange().getPaymentDirect(),
                    rabbitMQConfig.getRoutingKey().getRefundSuccess(),
                    message
                );
                log.info("退款成功消息已发送到RabbitMQ: messageId={}, orderId={}, tradeNo={}",
                    message.getMessageId(), paymentOrder.getOrderId(), paymentOrder.getTradeNo());
            } else {
                // 降级为日志记录
                log.info("RabbitMQ未启用，记录退款成功日志: {}", message.toString());
            }

        } catch (Exception e) {
            log.error("发送退款成功消息失败: orderId={}", paymentOrder.getOrderId(), e);
        }
    }

    /**
     * 构建支付成功消息
     */
    private PaymentMessage buildPaymentSuccessMessage(PaymentOrder paymentOrder) {
        PaymentMessage message = new PaymentMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setMessageType("PAYMENT_SUCCESS");
        message.setPaymentId(paymentOrder.getId());
        message.setOrderId(paymentOrder.getOrderId());
        message.setTradeNo(paymentOrder.getTradeNo());
        message.setAmount(paymentOrder.getAmount());
        message.setPaymentMethod(paymentOrder.getPaymentMethod());
        message.setStatus(paymentOrder.getStatusEnum() != null ? paymentOrder.getStatusEnum().name() : "UNKNOWN");
        message.setUserId(paymentOrder.getCreateBy());
        message.setTenantId(paymentOrder.getTenantId());
        message.setPaidTime(paymentOrder.getPaidTime());
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    /**
     * 构建支付失败消息
     */
    private PaymentMessage buildPaymentFailureMessage(PaymentOrder paymentOrder) {
        PaymentMessage message = new PaymentMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setMessageType("PAYMENT_FAILURE");
        message.setPaymentId(paymentOrder.getId());
        message.setOrderId(paymentOrder.getOrderId());
        message.setTradeNo(paymentOrder.getTradeNo());
        message.setAmount(paymentOrder.getAmount());
        message.setPaymentMethod(paymentOrder.getPaymentMethod());
        message.setStatus(paymentOrder.getStatusEnum() != null ? paymentOrder.getStatusEnum().name() : "UNKNOWN");
        message.setUserId(paymentOrder.getCreateBy());
        message.setTenantId(paymentOrder.getTenantId());
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    /**
     * 构建退款成功消息
     */
    private PaymentMessage buildRefundSuccessMessage(PaymentOrder paymentOrder) {
        PaymentMessage message = new PaymentMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setMessageType("REFUND_SUCCESS");
        message.setPaymentId(paymentOrder.getId());
        message.setOrderId(paymentOrder.getOrderId());
        message.setTradeNo(paymentOrder.getTradeNo());
        message.setAmount(paymentOrder.getAmount());
        message.setPaymentMethod(paymentOrder.getPaymentMethod());
        message.setStatus(paymentOrder.getStatusEnum() != null ? paymentOrder.getStatusEnum().name() : "UNKNOWN");
        message.setUserId(paymentOrder.getCreateBy());
        message.setTenantId(paymentOrder.getTenantId());
        message.setRefundAmount(paymentOrder.getRefundAmount());
        message.setRefundTime(paymentOrder.getRefundTime());
        message.setCreateTime(LocalDateTime.now());
        return message;
    }
}