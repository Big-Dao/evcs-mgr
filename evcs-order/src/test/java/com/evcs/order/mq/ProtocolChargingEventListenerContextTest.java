package com.evcs.order.mq;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.evcs.common.tenant.TenantContext;
import com.evcs.order.service.IChargingOrderService;
import com.evcs.protocol.event.StartEvent;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class ProtocolChargingEventListenerContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MDC.clear();
    }

    @Test
    @DisplayName("MQ listener - should set tenant + trace during handling and restore/clear after")
    void onStart_shouldSetTenantAndTrace_andRestoreAfter() throws IOException {
        IChargingOrderService chargingOrderService = mock(IChargingOrderService.class);
        ProtocolChargingEventListener listener = new ProtocolChargingEventListener(chargingOrderService);

        AtomicReference<Long> tenantInside = new AtomicReference<>();
        AtomicReference<Long> userInside = new AtomicReference<>();
        AtomicReference<String> traceInside = new AtomicReference<>();
        AtomicReference<String> requestInside = new AtomicReference<>();

        when(chargingOrderService.createOrderOnStart(
                anyLong(),
                anyLong(),
                anyString(),
                anyLong(),
                any()
        )).thenAnswer(invocation -> {
            tenantInside.set(TenantContext.getTenantId());
            userInside.set(TenantContext.getUserId());
            traceInside.set(MDC.get("traceId"));
            requestInside.set(MDC.get("requestId"));
            return true;
        });

        Channel channel = mock(Channel.class);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(99L);
        props.setMessageId("amqp-msg-1");
        Message amqpMessage = new Message(new byte[0], props);

        StartEvent event = StartEvent.builder()
                .eventId("evt-123")
                .tenantId(101L)
                .chargerId(202L)
                .sessionId("sess-1")
                .userId(303L)
                .stationId(404L)
                .eventTime(LocalDateTime.now())
                .build();

        // Pre-existing MDC should be restored afterwards
        MDC.put("traceId", "trace-prev");
        MDC.put("requestId", "req-prev");

        listener.onStart(event, amqpMessage, channel);

        // Inside handler, it should use eventId as traceId/requestId and set tenant/user
        assertEquals(101L, tenantInside.get());
        assertEquals(303L, userInside.get());
        assertEquals("evt-123", traceInside.get());
        assertEquals("evt-123", requestInside.get());

        // Ack happens
        verify(channel).basicAck(99L, false);

        // After handler returns: no tenant leak, and MDC restored
        assertNull(TenantContext.getTenantId());
        assertNull(TenantContext.getUserId());
        assertEquals("trace-prev", MDC.get("traceId"));
        assertEquals("req-prev", MDC.get("requestId"));
    }
}
