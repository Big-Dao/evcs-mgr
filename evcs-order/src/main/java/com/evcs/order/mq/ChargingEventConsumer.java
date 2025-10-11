package com.evcs.order.mq;

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

            // TODO: 实现订单创建逻辑
            // 1. 创建充电订单
            // 2. 记录开始时间
            // 3. 初始化计费信息
            
            log.info("Charging order created: sessionId={}, chargerId={}", 
                    event.getSessionId(), event.getChargerId());

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

            // TODO: 实现订单结束逻辑
            // 1. 更新订单状态为已完成
            // 2. 记录充电量和时长
            // 3. 计算费用
            // 4. 生成账单
            
            log.info("Charging order completed: sessionId={}, energy={}kWh, duration={}min", 
                    event.getSessionId(), event.getEnergy(), event.getDuration());

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
