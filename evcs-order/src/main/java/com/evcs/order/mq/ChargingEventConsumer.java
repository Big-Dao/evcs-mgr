package com.evcs.order.mq;

import com.evcs.common.context.TenantContext;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.service.IChargingOrderService;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StopEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 充电事件消费者（Order服务）
 * 消费充电开始和停止事件，处理订单业务逻辑
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargingEventConsumer {
    
    private final IChargingOrderService orderService;
    
    // 用于幂等性检查的事件ID集合（生产环境应使用Redis）
    private final Set<String> processedEventIds = new HashSet<>();

    /**
     * 消费充电开始事件
     * 创建或更新充电订单
     */
    @RabbitListener(queues = "evcs.protocol.charging")
    public void handleChargingStart(StartEvent event, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Received charging start event: chargerId={}, sessionId={}, userId={}", 
                    event.getChargerId(), event.getSessionId(), event.getUserId());

            // 幂等性检查
            if (processedEventIds.contains(event.getEventId())) {
                log.warn("Event already processed: eventId={}", event.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 设置租户上下文
            TenantContext.setCurrentTenantId(event.getTenantId());
            TenantContext.setCurrentUserId(event.getUserId());
            
            try {
                // 1. 幂等性检查：如果订单已存在则跳过
                ChargingOrder existingOrder = orderService.lambdaQuery()
                        .eq(ChargingOrder::getSessionId, event.getSessionId())
                        .one();
                
                if (existingOrder != null) {
                    log.info("Order already exists for sessionId={}, skipping", event.getSessionId());
                    processedEventIds.add(event.getEventId());
                    channel.basicAck(deliveryTag, false);
                    return;
                }
                
                // 2. 创建充电订单
                ChargingOrder order = new ChargingOrder();
                order.setChargerId(event.getChargerId());
                order.setUserId(event.getUserId());
                order.setSessionId(event.getSessionId());
                order.setOrderNo(event.getOrderNo());
                order.setStartTime(LocalDateTime.now());
                order.setStatus(1); // 充电中
                order.setInitialEnergy(event.getInitialEnergy());
                order.setTenantId(event.getTenantId());
                
                boolean saved = orderService.save(order);
                
                if (saved) {
                    log.info("Charging order created: orderId={}, sessionId={}, orderNo={}", 
                            order.getId(), event.getSessionId(), event.getOrderNo());
                } else {
                    log.error("Failed to create charging order: sessionId={}", event.getSessionId());
                }
            } finally {
                TenantContext.clear();
            }

            // 标记事件已处理
            processedEventIds.add(event.getEventId());

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to handle charging start event: {}", event, e);
            try {
                // 业务异常，不重新入队
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack message", ioException);
            }
        }
    }

    /**
     * 消费充电停止事件
     * 结束充电订单并计费
     */
    @RabbitListener(queues = "evcs.protocol.charging")
    public void handleChargingStop(StopEvent event, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Received charging stop event: chargerId={}, sessionId={}, energy={}, duration={}", 
                    event.getChargerId(), event.getSessionId(), event.getEnergy(), event.getDuration());

            // 幂等性检查
            if (processedEventIds.contains(event.getEventId())) {
                log.warn("Event already processed: eventId={}", event.getEventId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 设置租户上下文
            TenantContext.setCurrentTenantId(event.getTenantId());
            
            try {
                // 1. 查找订单
                ChargingOrder order = orderService.lambdaQuery()
                        .eq(ChargingOrder::getSessionId, event.getSessionId())
                        .one();
                
                if (order == null) {
                    log.warn("Order not found for sessionId={}, cannot update", event.getSessionId());
                    processedEventIds.add(event.getEventId());
                    channel.basicAck(deliveryTag, false);
                    return;
                }
                
                // 2. 更新订单状态为已完成
                order.setEndTime(LocalDateTime.now());
                order.setStatus(2); // 已完成
                order.setEnergy(event.getEnergy());
                order.setDuration(event.getDuration() != null ? event.getDuration().intValue() : null);
                order.setStopReason(event.getReason());
                
                // 3. 计算费用（TODO: 实现计费逻辑，这里先设置为0）
                // order.setTotalAmount(...);
                
                boolean updated = orderService.updateById(order);
                
                if (updated) {
                    log.info("Charging order completed: orderId={}, sessionId={}, energy={}kWh, duration={}min", 
                            order.getId(), event.getSessionId(), event.getEnergy(), event.getDuration());
                } else {
                    log.error("Failed to update charging order: orderId={}, sessionId={}", 
                            order.getId(), event.getSessionId());
                }
            } finally {
                TenantContext.clear();
            }

            // 标记事件已处理
            processedEventIds.add(event.getEventId());

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to handle charging stop event: {}", event, e);
            try {
                // 业务异常，不重新入队
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack message", ioException);
            }
        }
    }
}
