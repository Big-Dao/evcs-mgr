package com.evcs.payment.testsupport;

import com.evcs.payment.client.OrderServiceClient;
import com.evcs.payment.entity.PaymentOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only endpoint to trigger an outbound HTTP call.
 *
 * Loaded only in test classpath.
 */
@RestController
@RequestMapping("/__test/outbound")
@RequiredArgsConstructor
public class OutboundOrderCallbackTestController {

    private final OrderServiceClient orderServiceClient;

    @PostMapping("/order-callback")
    public ResponseEntity<String> triggerOrderCallback(@RequestParam(defaultValue = "true") boolean success) {
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderId(1001L);
        paymentOrder.setTradeNo("trade-test");

        // Intentionally do NOT set tenantId/createBy on paymentOrder,
        // so outbound headers must come from TenantContext/MDC.
        boolean ok = orderServiceClient.notifyPaymentCallback(paymentOrder, success);
        if (ok) {
            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.internalServerError().body("FAIL");
    }
}
