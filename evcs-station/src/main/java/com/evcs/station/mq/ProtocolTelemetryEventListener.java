package com.evcs.station.mq;

import com.evcs.common.trace.TraceMdc;
import com.evcs.common.tenant.TenantContext;
import com.evcs.protocol.event.TelemetryEvent;
import com.evcs.station.config.StationProtocolTelemetryQueueConfig;
import com.evcs.station.service.IChargerConnectorSessionCurveService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(
    queues = StationProtocolTelemetryQueueConfig.STATION_TELEMETRY_QUEUE,
    containerFactory = "protocolRabbitListenerContainerFactory"
)
public class ProtocolTelemetryEventListener {

    private final IChargerConnectorSessionCurveService sessionCurveService;

    @RabbitHandler
    public void onTelemetry(TelemetryEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        if (event == null || event.getTenantId() == null || event.getChargerId() == null) {
            log.warn("Invalid telemetry event payload, reject to DLQ: event={}", event);
            channel.basicReject(tag, false);
            return;
        }

        if (event.getConnectorId() == null || event.getConnectorId() <= 0) {
            channel.basicAck(tag, false);
            return;
        }

        String traceId = resolveTraceId(event.getEventId(), message, tag);
        try (TraceMdc ignored = TraceMdc.withTraceId(traceId)) {
            try {
                TenantContext.setCurrentTenantId(event.getTenantId());

                boolean ok = sessionCurveService != null && sessionCurveService.recordTelemetry(event);
                if (!ok) {
                    log.warn(
                        "Failed to persist telemetry event, reject to DLQ: tenantId={}, chargerId={}, connectorId={}, eventId={}",
                        event.getTenantId(),
                        event.getChargerId(),
                        event.getConnectorId(),
                        event.getEventId()
                    );
                    channel.basicReject(tag, false);
                    return;
                }

                channel.basicAck(tag, false);
            } catch (Exception ex) {
                log.error(
                    "Error handling telemetry event, nack to DLQ: tenantId={}, chargerId={}, eventId={}",
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

    @RabbitHandler(isDefault = true)
    public void onUnknown(Object event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        log.warn(
            "Unknown telemetry event type on station telemetry queue, reject to DLQ: payloadType={}",
            event == null ? "null" : event.getClass()
        );
        channel.basicReject(tag, false);
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
