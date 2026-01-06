package com.evcs.payment.scheduler;

import com.evcs.payment.config.PaymentConfig;
import com.evcs.payment.service.reconciliation.ProcessingPaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "evcs.payment.processing-reconcile", name = "enabled", havingValue = "true")
public class ProcessingPaymentReconciliationScheduler {

    private final ProcessingPaymentReconciliationService reconciliationService;
    private final PaymentConfig paymentConfig;

    @Scheduled(
        initialDelayString = "${evcs.payment.processing-reconcile.initial-delay-ms:60000}",
        fixedDelayString = "${evcs.payment.processing-reconcile.fixed-delay-ms:60000}"
    )
    public void run() {
        PaymentConfig.ProcessingReconcileConfig cfg = paymentConfig.getProcessingReconcile();
        if (!cfg.isEnabled()) {
            return;
        }

        try {
            reconciliationService.reconcileOnce();
        } catch (Exception ex) {
            log.error("PROCESSING订单轮询补偿任务执行失败", ex);
        }
    }
}
