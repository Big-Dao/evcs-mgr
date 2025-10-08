package com.evcs.station.mq;

import com.evcs.protocol.event.HeartbeatEvent;
import com.evcs.protocol.event.StatusEvent;
import com.evcs.station.entity.Charger;
import com.evcs.station.mapper.ChargerMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 协议事件消费者（Station服务）
 * 消费心跳和状态事件，更新充电桩状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProtocolEventConsumer {
    private final ChargerMapper chargerMapper;

    /**
     * 消费心跳事件
     * 更新充电桩最后心跳时间
     */
    @RabbitListener(queues = "evcs.protocol.heartbeat")
    public void handleHeartbeat(HeartbeatEvent event, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.debug("Received heartbeat event: chargerId={}, time={}", 
                    event.getChargerId(), event.getLastHeartbeatTime());

            // 更新充电桩心跳时间
            Charger charger = chargerMapper.selectById(event.getChargerId());
            if (charger != null) {
                chargerMapper.updateStatus(event.getChargerId(), charger.getStatus(), 
                        event.getLastHeartbeatTime());
                log.debug("Updated charger heartbeat: id={}", event.getChargerId());
            } else {
                log.warn("Charger not found: id={}", event.getChargerId());
            }

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to handle heartbeat event: {}", event, e);
            try {
                // 拒绝消息，不重新入队（避免死循环）
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack message", ioException);
            }
        }
    }

    /**
     * 消费状态变更事件
     * 更新充电桩状态
     */
    @RabbitListener(queues = "evcs.protocol.status")
    public void handleStatusChange(StatusEvent event, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("Received status event: chargerId={}, oldStatus={}, newStatus={}", 
                    event.getChargerId(), event.getOldStatus(), event.getNewStatus());

            // 更新充电桩状态
            chargerMapper.updateStatus(event.getChargerId(), event.getNewStatus(), 
                    event.getEventTime());
            log.info("Updated charger status: id={}, status={}", 
                    event.getChargerId(), event.getNewStatus());

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Failed to handle status event: {}", event, e);
            try {
                // 拒绝消息，不重新入队
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                log.error("Failed to nack message", ioException);
            }
        }
    }
}
