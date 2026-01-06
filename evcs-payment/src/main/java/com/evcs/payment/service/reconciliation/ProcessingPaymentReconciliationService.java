package com.evcs.payment.service.reconciliation;

/**
 * Background reconciliation for payment orders stuck in PROCESSING.
 */
public interface ProcessingPaymentReconciliationService {

    /**
     * Runs a single reconciliation pass.
     */
    ProcessingPaymentReconciliationResult reconcileOnce();

    class ProcessingPaymentReconciliationResult {
        private final int tenantCount;
        private final int scannedOrderCount;
        private final int finalizedOrderCount;
        private final int errorCount;

        public ProcessingPaymentReconciliationResult(
            int tenantCount,
            int scannedOrderCount,
            int finalizedOrderCount,
            int errorCount
        ) {
            this.tenantCount = tenantCount;
            this.scannedOrderCount = scannedOrderCount;
            this.finalizedOrderCount = finalizedOrderCount;
            this.errorCount = errorCount;
        }

        public int getTenantCount() {
            return tenantCount;
        }

        public int getScannedOrderCount() {
            return scannedOrderCount;
        }

        public int getFinalizedOrderCount() {
            return finalizedOrderCount;
        }

        public int getErrorCount() {
            return errorCount;
        }
    }
}
