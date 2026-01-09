package com.evcs.payment.scheduler;

import com.evcs.payment.config.PaymentConfig;
import com.evcs.payment.service.reconciliation.RefundingPaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "evcs.payment.refunding-reconcile", name = "enabled", havingValue = "true")
public class RefundingPaymentReconciliationScheduler {

    private final RefundingPaymentReconciliationService reconciliationService;
    private final PaymentConfig paymentConfig;

    @Scheduled(
        initialDelayString = "${evcs.payment.refunding-reconcile.initial-delay-ms:60000}",
        fixedDelayString = "${evcs.payment.refunding-reconcile.fixed-delay-ms:60000}"
    )
    public void run() {
        PaymentConfig.RefundingReconcileConfig cfg = paymentConfig.getRefundingReconcile();
        if (!cfg.isEnabled()) {
            return;
        }

        try {
            reconciliationService.reconcileOnce();
        } catch (Exception ex) {
            log.error("REFUNDING订单轮询补偿任务执行失败", ex);
        }
    }
}
