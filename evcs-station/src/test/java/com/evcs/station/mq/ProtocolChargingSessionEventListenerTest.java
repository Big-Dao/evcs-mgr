package com.evcs.station.mq;

import com.evcs.common.tenant.TenantContext;
import com.evcs.protocol.event.ProtocolEvent;
import com.evcs.protocol.event.StartEvent;
import com.evcs.protocol.event.StopEvent;
import com.evcs.station.service.IChargerConnectorService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("站点服务充电会话事件消费测试")
class ProtocolChargingSessionEventListenerTest {

    private Message messageWithTag(long tag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(tag);
        return new Message(new byte[0], props);
    }

    @Test
    @DisplayName("开始充电事件 - connectorId 存在且更新成功时应 ack")
    void testOnChargingStart_shouldAck_whenConnectorProvidedAndUpdateSucceeded() throws IOException {
        // Arrange
        IChargerConnectorService connectorService = Mockito.mock(IChargerConnectorService.class);
        Channel channel = Mockito.mock(Channel.class);
        ProtocolChargingSessionEventListener listener = new ProtocolChargingSessionEventListener(connectorService);

        StartEvent event = StartEvent.builder()
            .eventId("s-1")
            .tenantId(1L)
            .chargerId(10L)
            .connectorId(2)
            .eventType(ProtocolEvent.EventType.CHARGING_START)
            .eventTime(LocalDateTime.now())
            .protocolType("CLOUD_CHARGE")
            .sessionId("SESSION_1")
            .userId(100L)
            .initialEnergy(0.0)
            .success(true)
            .build();

        Mockito.when(connectorService.updateSessionStart(
            Mockito.eq(10L),
            Mockito.eq(2),
            Mockito.eq("SESSION_1"),
            Mockito.eq(100L),
            Mockito.any(),
            Mockito.eq(0.0)
        )).thenReturn(true);

        try {
            // Act
            listener.onChargingStart(event, messageWithTag(1L), channel);

            // Assert
            Mockito.verify(channel).basicAck(1L, false);
            assertNull(TenantContext.getCurrentTenantId(), "TenantContext should be cleared");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("停止充电事件 - connectorId 存在且更新成功时应 ack")
    void testOnChargingStop_shouldAck_whenConnectorProvidedAndUpdateSucceeded() throws IOException {
        // Arrange
        IChargerConnectorService connectorService = Mockito.mock(IChargerConnectorService.class);
        Channel channel = Mockito.mock(Channel.class);
        ProtocolChargingSessionEventListener listener = new ProtocolChargingSessionEventListener(connectorService);

        StopEvent event = StopEvent.builder()
            .eventId("t-1")
            .tenantId(1L)
            .chargerId(10L)
            .connectorId(1)
            .eventType(ProtocolEvent.EventType.CHARGING_STOP)
            .eventTime(LocalDateTime.now())
            .protocolType("CLOUD_CHARGE")
            .sessionId("SESSION_2")
            .energy(12.5)
            .duration(60L)
            .success(true)
            .build();

        Mockito.when(connectorService.updateSessionStop(
            Mockito.eq(10L),
            Mockito.eq(1),
            Mockito.eq("SESSION_2"),
            Mockito.eq(12.5),
            Mockito.eq(60L)
        )).thenReturn(true);

        try {
            // Act
            listener.onChargingStop(event, messageWithTag(2L), channel);

            // Assert
            Mockito.verify(channel).basicAck(2L, false);
            assertNull(TenantContext.getCurrentTenantId(), "TenantContext should be cleared");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("开始充电事件 - connectorId 缺失时应直接 ack")
    void testOnChargingStart_shouldAckAndSkip_whenConnectorMissing() throws IOException {
        // Arrange
        IChargerConnectorService connectorService = Mockito.mock(IChargerConnectorService.class);
        Channel channel = Mockito.mock(Channel.class);
        ProtocolChargingSessionEventListener listener = new ProtocolChargingSessionEventListener(connectorService);

        StartEvent event = StartEvent.builder()
            .eventId("s-2")
            .tenantId(1L)
            .chargerId(10L)
            .eventType(ProtocolEvent.EventType.CHARGING_START)
            .eventTime(LocalDateTime.now())
            .protocolType("CLOUD_CHARGE")
            .sessionId("SESSION_3")
            .userId(100L)
            .success(true)
            .build();

        try {
            // Act
            listener.onChargingStart(event, messageWithTag(3L), channel);

            // Assert
            Mockito.verify(channel).basicAck(3L, false);
            Mockito.verifyNoInteractions(connectorService);
            assertNull(TenantContext.getCurrentTenantId(), "TenantContext should be cleared");
        } finally {
            TenantContext.clear();
        }
    }
}
