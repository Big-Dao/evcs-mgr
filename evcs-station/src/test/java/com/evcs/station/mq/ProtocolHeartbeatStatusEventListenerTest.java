package com.evcs.station.mq;

import com.evcs.common.tenant.TenantContext;
import com.evcs.protocol.event.HeartbeatEvent;
import com.evcs.protocol.event.ProtocolEvent;
import com.evcs.protocol.event.StatusEvent;
import com.evcs.station.service.IChargerService;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("站点服务协议事件消费测试")
class ProtocolHeartbeatStatusEventListenerTest {

    private Message messageWithTag(long tag) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(tag);
        return new Message(new byte[0], props);
    }

    @Test
    @DisplayName("心跳事件 - 处理成功时应 ack")
    void testOnHeartbeat_shouldAck_whenUpdateSucceeded() throws IOException {
        // Arrange
        IChargerService chargerService = Mockito.mock(IChargerService.class);
        Channel channel = Mockito.mock(Channel.class);
        ProtocolHeartbeatStatusEventListener listener = new ProtocolHeartbeatStatusEventListener(chargerService);

        HeartbeatEvent event = HeartbeatEvent.builder()
                .eventId("e-1")
                .tenantId(1L)
                .chargerId(10L)
                .eventType(ProtocolEvent.EventType.HEARTBEAT)
                .eventTime(LocalDateTime.now())
                .protocolType("CLOUD_CHARGE")
                .lastHeartbeatTime(LocalDateTime.now())
                .build();

        Mockito.when(chargerService.updateHeartbeat(Mockito.eq(10L), Mockito.any())).thenReturn(true);

        try {
            // Act
            listener.onHeartbeat(event, messageWithTag(1L), channel);

            // Assert
            Mockito.verify(chargerService).updateHeartbeat(Mockito.eq(10L), Mockito.any());
            Mockito.verify(channel).basicAck(1L, false);
            assertNull(TenantContext.getCurrentTenantId(), "TenantContext should be cleared");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("心跳事件 - 更新失败时应 reject")
    void testOnHeartbeat_shouldReject_whenUpdateFailed() throws IOException {
        // Arrange
        IChargerService chargerService = Mockito.mock(IChargerService.class);
        Channel channel = Mockito.mock(Channel.class);
        ProtocolHeartbeatStatusEventListener listener = new ProtocolHeartbeatStatusEventListener(chargerService);

        HeartbeatEvent event = HeartbeatEvent.builder()
                .eventId("e-2")
                .tenantId(1L)
                .chargerId(10L)
                .eventType(ProtocolEvent.EventType.HEARTBEAT)
                .eventTime(LocalDateTime.now())
                .protocolType("CLOUD_CHARGE")
                .lastHeartbeatTime(LocalDateTime.now())
                .build();

        Mockito.when(chargerService.updateHeartbeat(Mockito.eq(10L), Mockito.any())).thenReturn(false);

        try {
            // Act
            listener.onHeartbeat(event, messageWithTag(2L), channel);

            // Assert
            Mockito.verify(channel).basicReject(2L, false);
            assertNull(TenantContext.getCurrentTenantId(), "TenantContext should be cleared");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    @DisplayName("状态事件 - 处理成功时应 ack")
    void testOnStatus_shouldAck_whenUpdateSucceeded() throws IOException {
        // Arrange
        IChargerService chargerService = Mockito.mock(IChargerService.class);
        Channel channel = Mockito.mock(Channel.class);
        ProtocolHeartbeatStatusEventListener listener = new ProtocolHeartbeatStatusEventListener(chargerService);

        StatusEvent event = StatusEvent.builder()
                .eventId("e-3")
                .tenantId(1L)
                .chargerId(10L)
                .eventType(ProtocolEvent.EventType.STATUS_CHANGE)
                .eventTime(LocalDateTime.now())
                .protocolType("CLOUD_CHARGE")
                .newStatus(2)
                .statusDesc("charging")
                .build();

        Mockito.when(chargerService.updateStatus(10L, 2)).thenReturn(true);

        try {
            // Act
            listener.onStatus(event, messageWithTag(3L), channel);

            // Assert
            Mockito.verify(chargerService).updateStatus(10L, 2);
            Mockito.verify(channel).basicAck(3L, false);
            assertNull(TenantContext.getCurrentTenantId(), "TenantContext should be cleared");
        } finally {
            TenantContext.clear();
        }
    }
}
