package com.evcs.station.mq;

import com.evcs.common.trace.TraceMdc;
import com.evcs.common.tenant.TenantContext;
import com.evcs.protocol.config.RabbitMQConfig;
import com.evcs.protocol.event.HeartbeatEvent;
import com.evcs.protocol.event.StatusEvent;
import com.evcs.station.service.IChargerConnectorService;
import com.evcs.station.service.IChargerService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProtocolHeartbeatStatusEventListener {

    private final IChargerService chargerService;
    private final IChargerConnectorService chargerConnectorService;

    @RabbitListener(
            queues = RabbitMQConfig.HEARTBEAT_QUEUE,
            containerFactory = "protocolRabbitListenerContainerFactory"
    )
    public void onHeartbeat(HeartbeatEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        if (event == null || event.getTenantId() == null || event.getChargerId() == null) {
            log.warn("Invalid heartbeat event payload, reject to DLQ: event={}", event);
            channel.basicReject(tag, false);
            return;
        }

        String traceId = resolveTraceId(event.getEventId(), message, tag);
        try (TraceMdc ignored = TraceMdc.withTraceId(traceId)) {
            try {
                TenantContext.setCurrentTenantId(event.getTenantId());

                LocalDateTime heartbeatTime = event.getLastHeartbeatTime() != null
                        ? event.getLastHeartbeatTime()
                        : event.getEventTime();
                boolean ok = chargerService.updateHeartbeat(event.getChargerId(), heartbeatTime);
                if (!ok) {
                    log.warn(
                            "Failed to update charger heartbeat, reject to DLQ: tenantId={}, chargerId={}, eventId={}",
                            event.getTenantId(),
                            event.getChargerId(),
                            event.getEventId()
                    );
                    channel.basicReject(tag, false);
                    return;
                }

                // Best-effort: also touch all connector heartbeats for this charger.
                try {
                    if (chargerConnectorService != null) {
                        chargerConnectorService.touchAllHeartbeat(event.getChargerId(), heartbeatTime);
                    }
                } catch (Exception e) {
                    log.debug("Failed to touch connector heartbeats: tenantId={}, chargerId={}", event.getTenantId(), event.getChargerId(), e);
                }

                channel.basicAck(tag, false);
            } catch (Exception ex) {
                log.error(
                        "Error handling heartbeat event, nack to DLQ: tenantId={}, chargerId={}, eventId={}",
                        event.getTenantId(),
                        event.getChargerId(),
                        event.getEventId(),
                        ex
                );
                channel.basicNack(tag, false, false);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @RabbitListener(
            queues = RabbitMQConfig.STATUS_QUEUE,
            containerFactory = "protocolRabbitListenerContainerFactory"
    )
    public void onStatus(StatusEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        if (event == null || event.getTenantId() == null || event.getChargerId() == null || event.getNewStatus() == null) {
            log.warn("Invalid status event payload, reject to DLQ: event={}", event);
            channel.basicReject(tag, false);
            return;
        }

        String traceId = resolveTraceId(event.getEventId(), message, tag);
        try (TraceMdc ignored = TraceMdc.withTraceId(traceId)) {
            try {
                TenantContext.setCurrentTenantId(event.getTenantId());

                LocalDateTime eventTime = event.getEventTime() != null ? event.getEventTime() : LocalDateTime.now();

                boolean ok;
                if (event.getConnectorId() != null && event.getConnectorId() > 0) {
                    ok = chargerConnectorService != null && chargerConnectorService.updateStatus(
                        event.getChargerId(),
                        event.getConnectorId(),
                        event.getNewStatus(),
                        event.getFaultCode(),
                        event.getFaultDescription(),
                        eventTime
                    );
                } else {
                    ok = chargerService.updateStatus(event.getChargerId(), event.getNewStatus());
                }
                if (!ok) {
                    log.warn(
                            "Failed to update status, reject to DLQ: tenantId={}, chargerId={}, connectorId={}, newStatus={}, eventId={}",
                            event.getTenantId(),
                            event.getChargerId(),
                            event.getConnectorId(),
                            event.getNewStatus(),
                            event.getEventId()
                    );
                    channel.basicReject(tag, false);
                    return;
                }

                channel.basicAck(tag, false);
            } catch (Exception ex) {
                log.error(
                        "Error handling status event, nack to DLQ: tenantId={}, chargerId={}, eventId={}",
                        event.getTenantId(),
                        event.getChargerId(),
                        event.getEventId(),
                        ex
                );
                channel.basicNack(tag, false, false);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private static String resolveTraceId(String eventId, Message message, long deliveryTag) {
        if (eventId != null && !eventId.isBlank()) {
            return eventId;
        }

        String messageId = message.getMessageProperties().getMessageId();
        if (messageId != null && !messageId.isBlank()) {
            return messageId;
        }

        return "mq-" + deliveryTag;
    }
}
