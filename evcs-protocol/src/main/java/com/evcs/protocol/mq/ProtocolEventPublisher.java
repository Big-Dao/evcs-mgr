package com.evcs.protocol.mq;

import com.evcs.protocol.config.RabbitMQConfig;
import com.evcs.protocol.event.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 协议事件发布者
 * 负责将协议事件发送到RabbitMQ
 * 在测试环境中，如果RabbitTemplate不可用，会记录日志但不抛出异常
 */
@Slf4j
@Service
public class ProtocolEventPublisher {

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    // In-memory bounded history of recent protocol events. This is intended for debugging
    // and test inspection only and is not a substitute for persistent storage or audit logs.
    private final java.util.concurrent.ConcurrentLinkedDeque<
        ProtocolEvent
    > eventHistory = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private static final int MAX_HISTORY_SIZE = 100;

    /**
     * 发布心跳事件
     */
    public HeartbeatEvent publishHeartbeat(
        Long chargerId,
        Long tenantId,
        String protocolType,
        LocalDateTime heartbeatTime
    ) {
        HeartbeatEvent event = HeartbeatEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .chargerId(chargerId)
            .tenantId(tenantId)
            .eventType(ProtocolEvent.EventType.HEARTBEAT)
            .eventTime(LocalDateTime.now())
            .protocolType(protocolType)
            .lastHeartbeatTime(heartbeatTime)
            .build();

        publishEvent(event);
        return event;
    }

    /**
     * 发布状态变更事件
     */
    public StatusEvent publishStatusChange(
        Long chargerId,
        Long tenantId,
        String protocolType,
        Integer oldStatus,
        Integer newStatus,
        String statusDesc
    ) {
        StatusEvent event = StatusEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .chargerId(chargerId)
            .tenantId(tenantId)
            .eventType(ProtocolEvent.EventType.STATUS_CHANGE)
            .eventTime(LocalDateTime.now())
            .protocolType(protocolType)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .statusDesc(statusDesc)
            .build();

        publishEvent(event);
        return event;
    }

    /**
     * 发布开始充电事件
     */
    public StartEvent publishChargingStart(
        Long chargerId,
        Long tenantId,
        String protocolType,
        String sessionId,
        Long userId,
        String orderNo,
        Double initialEnergy,
        Boolean success,
        String message
    ) {
        StartEvent event = StartEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .chargerId(chargerId)
            .tenantId(tenantId)
            .eventType(ProtocolEvent.EventType.CHARGING_START)
            .eventTime(LocalDateTime.now())
            .protocolType(protocolType)
            .sessionId(sessionId)
            .userId(userId)
            .orderNo(orderNo)
            .initialEnergy(initialEnergy)
            .success(success)
            .message(message)
            .build();

        publishEvent(event);
        return event;
    }

    /**
     * 发布停止充电事件
     */
    public StopEvent publishChargingStop(
        Long chargerId,
        Long tenantId,
        String protocolType,
        String sessionId,
        String orderNo,
        Double energy,
        Long duration,
        String reason,
        Boolean success,
        String message
    ) {
        StopEvent event = StopEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .chargerId(chargerId)
            .tenantId(tenantId)
            .eventType(ProtocolEvent.EventType.CHARGING_STOP)
            .eventTime(LocalDateTime.now())
            .protocolType(protocolType)
            .sessionId(sessionId)
            .orderNo(orderNo)
            .energy(energy)
            .duration(duration)
            .reason(reason)
            .success(success)
            .message(message)
            .build();

        publishEvent(event);
        return event;
    }

    /**
     * 获取最近的事件历史（用于调试/测试）
     * 返回的是事件列表的快照；修改返回列表不会影响内部缓冲。
     */
    public java.util.List<ProtocolEvent> getEventHistory() {
        return new java.util.ArrayList<>(eventHistory);
    }

    /**
     * 发布事件到RabbitMQ
     */
    private void publishEvent(ProtocolEvent event) {
        // Record the event in in-memory bounded history for debugging/tests.
        try {
            eventHistory.addFirst(event);
            while (eventHistory.size() > MAX_HISTORY_SIZE) {
                eventHistory.pollLast();
            }
        } catch (Exception ex) {
            // History recording must never prevent event publishing.
            log.warn(
                "Failed to record protocol event in in-memory history: {}",
                ex.getMessage()
            );
        }

        if (rabbitTemplate == null) {
            log.warn(
                "RabbitTemplate is not available (likely in test environment). Event will not be published: type={}, chargerId={}",
                event.getEventType(),
                event.getChargerId()
            );
            return;
        }

        try {
            String routingKey = event.getRoutingKey();
            log.info(
                "Publishing protocol event: type={}, chargerId={}, routingKey={}",
                event.getEventType(),
                event.getChargerId(),
                routingKey
            );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PROTOCOL_EXCHANGE,
                routingKey,
                event
            );

            log.debug(
                "Event published successfully: eventId={}",
                event.getEventId()
            );
        } catch (Exception e) {
            log.error(
                "Failed to publish event: eventId={}, type={}, chargerId={}",
                event.getEventId(),
                event.getEventType(),
                event.getChargerId(),
                e
            );
            throw new RuntimeException("Failed to publish protocol event", e);
        }
    }
}
