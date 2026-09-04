package com.evcs.order.mq;

import com.evcs.common.trace.TraceMdc;
import com.evcs.common.tenant.TenantContext;
import com.evcs.protocol.mq.ProtocolMqConstants;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StopEvent;
import com.evcs.protocol.event.ProtocolEvent;
import com.evcs.order.service.IChargingOrderService;
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
        queues = ProtocolMqConstants.CHARGING_QUEUE,
        containerFactory = "protocolRabbitListenerContainerFactory"
)
public class ProtocolChargingEventListener {

    private final IChargingOrderService chargingOrderService;

    @RabbitHandler
    public void onStart(StartEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        if (event == null || event.getTenantId() == null || event.getChargerId() == null || event.getSessionId() == null) {
            log.warn("Invalid start event payload, reject to DLQ: event={}", event);
            channel.basicReject(tag, false);
            return;
        }

        if (event.getSuccess() != null && !event.getSuccess()) {
            log.warn("Start event indicates failure, ack and skip: eventId={}, message={}", event.getEventId(), event.getMessage());
            channel.basicAck(tag, false);
            return;
        }

        if (event.getUserId() == null) {
            log.warn("Start event missing userId, reject to DLQ: eventId={}", event.getEventId());
            channel.basicReject(tag, false);
            return;
        }

        if (event.getStationId() == null) {
            log.warn("Start event missing stationId, reject to DLQ: eventId={}", event.getEventId());
            channel.basicReject(tag, false);
            return;
        }

        String traceId = resolveTraceId(event.getEventId(), message, tag);
        try (TraceMdc ignored = TraceMdc.withTraceId(traceId)) {
            try {
                TenantContext.setCurrentTenantId(event.getTenantId());
                TenantContext.setUserId(event.getUserId());

                boolean ok = chargingOrderService.createOrderOnStart(
                        event.getStationId(),
                        event.getChargerId(),
                        event.getSessionId(),
                        event.getUserId(),
                        event.getBillingPlanId()
                );

                if (!ok) {
                    log.error(
                            "Order create on start returned false, reject to DLQ: tenantId={}, sessionId={}, eventId={}",
                            event.getTenantId(),
                            event.getSessionId(),
                            event.getEventId()
                    );
                    channel.basicReject(tag, false);
                    return;
                }

                channel.basicAck(tag, false);
            } catch (Exception ex) {
                log.error(
                        "Error handling start event, nack to DLQ: tenantId={}, sessionId={}, eventId={}",
                        event.getTenantId(),
                        event.getSessionId(),
                        event.getEventId(),
                        ex
                );
                channel.basicNack(tag, false, false);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @RabbitHandler
    public void onStop(StopEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        if (event == null || event.getTenantId() == null || event.getSessionId() == null) {
            log.warn("Invalid stop event payload, reject to DLQ: event={}", event);
            channel.basicReject(tag, false);
            return;
        }

        if (event.getSuccess() != null && !event.getSuccess()) {
            log.warn("Stop event indicates failure, ack and skip: eventId={}, message={}", event.getEventId(), event.getMessage());
            channel.basicAck(tag, false);
            return;
        }

        String traceId = resolveTraceId(event.getEventId(), message, tag);
        try (TraceMdc ignored = TraceMdc.withTraceId(traceId)) {
            try {
                TenantContext.setCurrentTenantId(event.getTenantId());

                boolean ok = chargingOrderService.completeOrderOnStop(
                        event.getSessionId(),
                        event.getEnergy(),
                        event.getDuration()
                );

                if (!ok) {
                    log.error(
                            "Order complete on stop returned false, reject to DLQ: tenantId={}, sessionId={}, eventId={}",
                            event.getTenantId(),
                            event.getSessionId(),
                            event.getEventId()
                    );
                    channel.basicReject(tag, false);
                    return;
                }

                channel.basicAck(tag, false);
            } catch (Exception ex) {
                log.error(
                        "Error handling stop event, nack to DLQ: tenantId={}, sessionId={}, eventId={}",
                        event.getTenantId(),
                        event.getSessionId(),
                        event.getEventId(),
                        ex
                );
                channel.basicNack(tag, false, false);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @RabbitHandler
    public void onUnknown(ProtocolEvent event, Message message, Channel channel) throws IOException {
        long tag = message.getMessageProperties().getDeliveryTag();
        log.warn("Unknown protocol event type received on charging queue, reject to DLQ: {}", event);
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
