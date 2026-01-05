package com.evcs.station.mq;

import com.evcs.common.tenant.TenantContext;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StopEvent;
import com.evcs.station.config.StationProtocolChargingQueueConfig;
import com.evcs.station.service.IChargerConnectorService;
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
    queues = StationProtocolChargingQueueConfig.STATION_CHARGING_QUEUE,
    containerFactory = "protocolRabbitListenerContainerFactory"
)
public class ProtocolChargingSessionEventListener {

    private final IChargerConnectorService chargerConnectorService;
    private final IChargerConnectorSessionCurveService sessionCurveService;

    @RabbitHandler
    public void onChargingStart(StartEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        if (event == null || event.getTenantId() == null || event.getChargerId() == null) {
            log.warn("Invalid charging start event payload, reject to DLQ: event={}", event);
            channel.basicReject(tag, false);
            return;
        }

        if (event.getConnectorId() == null || event.getConnectorId() <= 0) {
            log.debug(
                "Charging start event has no connectorId, ack and skip connector session update: tenantId={}, chargerId={}, eventId={}",
                event.getTenantId(),
                event.getChargerId(),
                event.getEventId()
            );
            channel.basicAck(tag, false);
            return;
        }

        try {
            TenantContext.setCurrentTenantId(event.getTenantId());

            boolean ok = chargerConnectorService != null && chargerConnectorService.updateSessionStart(
                event.getChargerId(),
                event.getConnectorId(),
                event.getSessionId(),
                event.getUserId(),
                event.getEventTime(),
                event.getInitialEnergy()
            );

            // Best-effort: record session history for later curve browsing.
            try {
                if (sessionCurveService != null) {
                    sessionCurveService.recordSessionStart(event);
                }
            } catch (Exception e) {
                log.debug(
                    "Failed to record connector session history on start: tenantId={}, chargerId={}, connectorId={}, sessionId={}",
                    event.getTenantId(),
                    event.getChargerId(),
                    event.getConnectorId(),
                    event.getSessionId(),
                    e
                );
            }

            if (!ok) {
                log.warn(
                    "Failed to update connector session start, reject to DLQ: tenantId={}, chargerId={}, connectorId={}, sessionId={}, eventId={}",
                    event.getTenantId(),
                    event.getChargerId(),
                    event.getConnectorId(),
                    event.getSessionId(),
                    event.getEventId()
                );
                channel.basicReject(tag, false);
                return;
            }

            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.error(
                "Error handling charging start event, nack to DLQ: tenantId={}, chargerId={}, eventId={}",
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

    @RabbitHandler
    public void onChargingStop(StopEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        if (event == null || event.getTenantId() == null || event.getChargerId() == null) {
            log.warn("Invalid charging stop event payload, reject to DLQ: event={}", event);
            channel.basicReject(tag, false);
            return;
        }

        if (event.getConnectorId() == null || event.getConnectorId() <= 0) {
            log.debug(
                "Charging stop event has no connectorId, ack and skip connector session update: tenantId={}, chargerId={}, eventId={}",
                event.getTenantId(),
                event.getChargerId(),
                event.getEventId()
            );
            channel.basicAck(tag, false);
            return;
        }

        try {
            TenantContext.setCurrentTenantId(event.getTenantId());

            boolean ok = chargerConnectorService != null && chargerConnectorService.updateSessionStop(
                event.getChargerId(),
                event.getConnectorId(),
                event.getSessionId(),
                event.getEnergy(),
                event.getDuration()
            );

            // Best-effort: record session history completion.
            try {
                if (sessionCurveService != null) {
                    sessionCurveService.recordSessionStop(event);
                }
            } catch (Exception e) {
                log.debug(
                    "Failed to record connector session history on stop: tenantId={}, chargerId={}, connectorId={}, sessionId={}",
                    event.getTenantId(),
                    event.getChargerId(),
                    event.getConnectorId(),
                    event.getSessionId(),
                    e
                );
            }

            if (!ok) {
                log.warn(
                    "Failed to update connector session stop, reject to DLQ: tenantId={}, chargerId={}, connectorId={}, sessionId={}, eventId={}",
                    event.getTenantId(),
                    event.getChargerId(),
                    event.getConnectorId(),
                    event.getSessionId(),
                    event.getEventId()
                );
                channel.basicReject(tag, false);
                return;
            }

            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.error(
                "Error handling charging stop event, nack to DLQ: tenantId={}, chargerId={}, eventId={}",
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

    @RabbitHandler(isDefault = true)
    public void onUnknown(Object event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        log.warn("Unknown charging event type on station charging queue, reject to DLQ: payloadType={}", event == null ? "null" : event.getClass());
        channel.basicReject(tag, false);
    }
}
