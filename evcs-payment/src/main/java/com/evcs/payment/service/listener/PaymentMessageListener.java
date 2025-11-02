package com.evcs.payment.service.listener;

import com.evcs.payment.dto.message.PaymentMessage;
import com.evcs.payment.service.message.PaymentMessageService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 支付消息监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageListener {

    private final PaymentMessageService paymentMessageService;

    /**
     * 监听支付成功消息
     */
    @RabbitListener(queues = "${evcs.payment.rabbitmq.queue.payment-success}")
    public void handlePaymentSuccessMessage(@Payload PaymentMessage message,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                          Channel channel,
                                          Message amqpMessage) {
        log.info("接收到支付成功消息: messageId={}, orderId={}, tradeNo={}",
                message.getMessageId(), message.getOrderId(), message.getTradeNo());

        try {
            // 处理支付成功消息
            processPaymentSuccessMessage(message);

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("支付成功消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("处理支付成功消息失败: messageId={}", message.getMessageId(), e);
            handleProcessingFailure(message, channel, deliveryTag, e);
        }
    }

    /**
     * 监听支付失败消息
     */
    @RabbitListener(queues = "${evcs.payment.rabbitmq.queue.payment-failure}")
    public void handlePaymentFailureMessage(@Payload PaymentMessage message,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                          Channel channel,
                                          Message amqpMessage) {
        log.info("接收到支付失败消息: messageId={}, orderId={}, tradeNo={}",
                message.getMessageId(), message.getOrderId(), message.getTradeNo());

        try {
            // 处理支付失败消息
            processPaymentFailureMessage(message);

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("支付失败消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("处理支付失败消息失败: messageId={}", message.getMessageId(), e);
            handleProcessingFailure(message, channel, deliveryTag, e);
        }
    }

    /**
     * 监听退款成功消息
     */
    @RabbitListener(queues = "${evcs.payment.rabbitmq.queue.refund-success}")
    public void handleRefundSuccessMessage(@Payload PaymentMessage message,
                                         @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                         Channel channel,
                                         Message amqpMessage) {
        log.info("接收到退款成功消息: messageId={}, orderId={}, tradeNo={}",
                message.getMessageId(), message.getOrderId(), message.getTradeNo());

        try {
            // 处理退款成功消息
            processRefundSuccessMessage(message);

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("退款成功消息处理完成: messageId={}", message.getMessageId());

        } catch (Exception e) {
            log.error("处理退款成功消息失败: messageId={}", message.getMessageId(), e);
            handleProcessingFailure(message, channel, deliveryTag, e);
        }
    }

    /**
     * 监听死信消息
     */
    @RabbitListener(queues = "${evcs.payment.rabbitmq.queue.payment-dlq}")
    public void handleDeadLetterMessage(@Payload PaymentMessage message,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                      Channel channel,
                                      Message amqpMessage,
                                      @Header(value = "x-death", required = false) Object deathHeader) {
        log.error("接收到死信消息: messageId={}, messageType={}, retryCount={}",
                message.getMessageId(), message.getMessageType(), message.getRetryCount());

        try {
            // 记录死信消息，用于人工干预
            processDeadLetterMessage(message, deathHeader);

            // 手动确认死信消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("处理死信消息失败: messageId={}", message.getMessageId(), e);
            try {
                // 确认死信消息，避免重复处理
                channel.basicAck(deliveryTag, false);
            } catch (IOException ioException) {
                log.error("确认死信消息失败", ioException);
            }
        }
    }

    /**
     * 处理支付成功消息
     */
    private void processPaymentSuccessMessage(PaymentMessage message) {
        log.info("处理支付成功消息: orderId={}, amount={}", message.getOrderId(), message.getAmount());

        // 这里可以添加具体的业务逻辑，例如：
        // 1. 更新业务订单状态
        // 2. 发送通知给用户
        // 3. 触发后续业务流程
        // 4. 记录审计日志等

        // 示例：模拟业务处理
        log.info("模拟处理支付成功业务逻辑 - 订单ID: {}, 金额: {}", message.getOrderId(), message.getAmount());
    }

    /**
     * 处理支付失败消息
     */
    private void processPaymentFailureMessage(PaymentMessage message) {
        log.info("处理支付失败消息: orderId={}, amount={}", message.getOrderId(), message.getAmount());

        // 这里可以添加具体的业务逻辑，例如：
        // 1. 更新业务订单状态为支付失败
        // 2. 释放库存
        // 3. 发送支付失败通知
        // 4. 记录支付失败原因等

        log.info("模拟处理支付失败业务逻辑 - 订单ID: {}, 金额: {}", message.getOrderId(), message.getAmount());
    }

    /**
     * 处理退款成功消息
     */
    private void processRefundSuccessMessage(PaymentMessage message) {
        log.info("处理退款成功消息: orderId={}, refundAmount={}", message.getOrderId(), message.getRefundAmount());

        // 这里可以添加具体的业务逻辑，例如：
        // 1. 更新订单退款状态
        // 2. 处理退款到账
        // 3. 发送退款通知
        // 4. 记录退款日志等

        log.info("模拟处理退款成功业务逻辑 - 订单ID: {}, 退款金额: {}", message.getOrderId(), message.getRefundAmount());
    }

    /**
     * 处理死信消息
     */
    private void processDeadLetterMessage(PaymentMessage message, Object deathHeader) {
        log.error("处理死信消息: messageId={}, messageType={}, deathHeader={}",
                message.getMessageId(), message.getMessageType(), deathHeader);

        // 这里可以添加死信消息处理逻辑，例如：
        // 1. 记录到专门的死信消息表
        // 2. 发送告警通知
        // 3. 等待人工干预
        // 4. 尝试重新处理等

        log.error("模拟处理死信消息业务逻辑 - 消息ID: {}, 类型: {}", message.getMessageId(), message.getMessageType());
    }

    /**
     * 处理消息处理失败
     */
    private void handleProcessingFailure(PaymentMessage message, Channel channel, long deliveryTag, Exception e) {
        try {
            // 增加重试次数
            message.setRetryCount(message.getRetryCount() + 1);

            // 检查是否超过最大重试次数
            if (message.getRetryCount() >= message.getMaxRetries()) {
                log.error("消息重试次数超过最大限制，拒绝消息: messageId={}, retryCount={}",
                        message.getMessageId(), message.getRetryCount());

                // 拒绝消息，进入死信队列
                channel.basicNack(deliveryTag, false, false);
            } else {
                log.warn("消息处理失败，准备重试: messageId={}, retryCount={}",
                        message.getMessageId(), message.getRetryCount());

                // 拒绝消息并重新入队（注意：这种方式可能导致立即重试，生产环境建议使用延迟队列）
                channel.basicNack(deliveryTag, false, true);
            }

        } catch (IOException ioException) {
            log.error("处理消息失败时发生IO异常", ioException);
            try {
                // 最后的确认，避免消息堆积
                channel.basicAck(deliveryTag, false);
            } catch (IOException ex) {
                log.error("确认消息失败", ex);
            }
        }
    }
}