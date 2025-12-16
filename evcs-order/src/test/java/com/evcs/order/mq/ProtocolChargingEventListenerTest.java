package com.evcs.order.mq;

import com.evcs.common.tenant.TenantContext;
import com.evcs.protocol.event.ProtocolEvent;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StopEvent;
import com.evcs.order.service.IChargingOrderService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("订单服务协议充电事件消费测试")
class ProtocolChargingEventListenerTest {

    private Message messageWithTag(long tag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(tag);
        return new Message(new byte[0], props);
    }

    @Test
    @DisplayName("开始充电 - 字段齐全且创建成功时应 ack")
    void testOnStart_shouldAck_whenCreateSucceeded() throws IOException {
        // Arrange
        IChargingOrderService orderService = Mockito.mock(IChargingOrderService.class);
        Channel channel = Mockito.mock(Channel.class);
        ProtocolChargingEventListener listener = new ProtocolChargingEventListener(orderService);

        StartEvent event = StartEvent.builder()
                .eventId("s-1")
                .tenantId(1L)
                .stationId(11L)
                .chargerId(10L)
                .eventType(ProtocolEvent.EventType.CHARGING_START)
                .eventTime(LocalDateTime.now())
                .protocolType("CLOUD_CHARGE")
                .sessionId("SESSION_001")
                .userId(100L)
                .billingPlanId(200L)
                .success(true)
                .message("ok")
                .build();

        Mockito.when(orderService.createOrderOnStart(11L, 10L, "SESSION_001", 100L, 200L)).thenReturn(true);

        try {
            // Act
            listener.onStart(event, messageWithTag(1L), channel);

            // Assert
            Mockito.verify(channel).basicAck(1L, false);
            assertNull(TenantContext.getCurrentTenantId(), "TenantContext should be cleared");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("开始充电 - 缺少 stationId 时应 reject")
    void testOnStart_shouldReject_whenStationIdMissing() throws IOException {
        // Arrange
        IChargingOrderService orderService = Mockito.mock(IChargingOrderService.class);
        Channel channel = Mockito.mock(Channel.class);
        ProtocolChargingEventListener listener = new ProtocolChargingEventListener(orderService);

        StartEvent event = StartEvent.builder()
                .eventId("s-2")
                .tenantId(1L)
                .chargerId(10L)
                .eventType(ProtocolEvent.EventType.CHARGING_START)
                .eventTime(LocalDateTime.now())
                .protocolType("CLOUD_CHARGE")
                .sessionId("SESSION_002")
                .userId(100L)
                .success(true)
                .build();

        try {
            // Act
            listener.onStart(event, messageWithTag(2L), channel);

            // Assert
            Mockito.verify(channel).basicReject(2L, false);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("停止充电 - 完成成功时应 ack")
    void testOnStop_shouldAck_whenCompleteSucceeded() throws IOException {
        // Arrange
        IChargingOrderService orderService = Mockito.mock(IChargingOrderService.class);
        Channel channel = Mockito.mock(Channel.class);
        ProtocolChargingEventListener listener = new ProtocolChargingEventListener(orderService);

        StopEvent event = StopEvent.builder()
                .eventId("t-1")
                .tenantId(1L)
                .chargerId(10L)
                .eventType(ProtocolEvent.EventType.CHARGING_STOP)
                .eventTime(LocalDateTime.now())
                .protocolType("CLOUD_CHARGE")
                .sessionId("SESSION_001")
                .energy(12.3)
                .duration(15L)
                .success(true)
                .build();

        Mockito.when(orderService.completeOrderOnStop("SESSION_001", 12.3, 15L)).thenReturn(true);

        try {
            // Act
            listener.onStop(event, messageWithTag(3L), channel);

            // Assert
            Mockito.verify(channel).basicAck(3L, false);
            assertNull(TenantContext.getCurrentTenantId(), "TenantContext should be cleared");
        } finally {
            TenantContext.clear();
        }
    }
}
