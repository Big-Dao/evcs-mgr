package com.evcs.integration.test;

import com.evcs.common.test.base.BaseIntegrationTest;
import com.evcs.common.test.util.TestDataFactory;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.service.IChargingOrderService;
import com.evcs.payment.dto.PaymentRequest;
import com.evcs.payment.dto.PaymentResponse;
import com.evcs.payment.service.IPaymentService;
import com.evcs.station.entity.Charger;
import com.evcs.station.entity.Station;
import com.evcs.station.service.IChargerService;
import com.evcs.station.service.IStationService;
import com.evcs.order.controller.OrderController;
import com.evcs.order.dto.PayParams;
import com.evcs.payment.enums.PaymentMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {
    com.evcs.station.StationServiceApplication.class,
    com.evcs.order.OrderServiceApplication.class,
    com.evcs.payment.PaymentServiceApplication.class,
    com.evcs.integration.config.TestConfig.class
}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "evcs.payment.order-sync.direct-api-enabled=true",
    "evcs.payment.order-sync.order-service-url=http://localhost:8080", // Placeholder, will be mocked
    "spring.rabbitmq.listener.simple.auto-startup=false",
    "spring.cloud.discovery.enabled=false"
})
@DisplayName("全链路集成测试")
class FullFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IStationService stationService;
    @Autowired
    private IChargerService chargerService;
    @Autowired
    private IChargingOrderService orderService;
    @Autowired
    private IPaymentService paymentService;
    @Autowired
    private OrderController orderController;

    @MockBean(name = "orderSyncRestTemplate")
    private RestTemplate restTemplate;

    @Test
    @DisplayName("完整流程：建站->充电->支付->回调")
    void testFullFlow() {
        // Mock RestTemplate to call OrderController directly
        Mockito.when(restTemplate.postForEntity(any(String.class), any(), any()))
            .thenAnswer(invocation -> {
                // Simulate successful callback
                return ResponseEntity.ok("true");
            });

        // 1. Setup Station & Charger
        Station station = new Station();
        station.setStationCode(TestDataFactory.generateCode("STATION"));
        station.setStationName("FullFlow Station");
        station.setLatitude(39.9042);
        station.setLongitude(116.4074);
        station.setStatus(1);
        stationService.saveStation(station);

        Charger charger = new Charger();
        charger.setStationId(station.getStationId());
        charger.setChargerCode(TestDataFactory.generateCode("CHARGER"));
        charger.setStatus(1);
        chargerService.saveCharger(charger);

        // 2. Start Charging (Create Order)
        String sessionId = UUID.randomUUID().toString();
        Long userId = 10086L;
        boolean started = orderService.createOrderOnStart(station.getStationId(), charger.getId(), sessionId, userId, null);
        assertTrue(started, "Order should be created");

        // 3. Stop Charging (Complete Order)
        boolean stopped = orderService.completeOrderOnStop(sessionId, 20.5, 3600L);
        assertTrue(stopped, "Order should be completed");

        ChargingOrder order = orderService.getBySessionId(sessionId);
        assertNotNull(order);
        // Status 1 = UNPAID/COMPLETED
        assertEquals(1, order.getStatus()); 

        // 4. Prepare for Payment (Generate Trade ID)
        PayParams payParams = orderService.createPayment(order.getId());
        assertNotNull(payParams);
        String tradeId = payParams.getTradeId();
        assertNotNull(tradeId);

        // 5. Create Payment
        PaymentRequest payReq = new PaymentRequest();
        payReq.setOrderId(order.getId());
        payReq.setAmount(new BigDecimal("10.00")); 
        payReq.setPaymentMethod(PaymentMethod.ALIPAY_APP);
        payReq.setDescription("EV Charging");
        payReq.setTradeNo(tradeId); // Pass the tradeId from Order Service
        payReq.setUserId(1001L);
        
        PaymentResponse payResp = paymentService.createPayment(payReq);
        assertNotNull(payResp);
        assertEquals(tradeId, payResp.getTradeNo());

        // 6. Payment Callback
        boolean handled = paymentService.handlePaymentCallback(tradeId, true);
        assertTrue(handled, "Callback should be handled");
        
        // Manually trigger the order update because RestTemplate was mocked and we want to ensure the loop closes
        // In real env, OrderSyncService calls this via HTTP
        orderController.paymentCallback(tradeId, true);

        // 7. Verify Final Order Status
        ChargingOrder finalOrder = orderService.getById(order.getId());
        // Status 11 = PAID
        assertEquals(11, finalOrder.getStatus()); 
    }
}
