package com.evcs.payment.service.reconciliation;

/**
 * Background reconciliation for payment orders stuck in REFUNDING.
 */
public interface RefundingPaymentReconciliationService {

    /**
     * Runs a single reconciliation pass.
     */
    RefundingPaymentReconciliationResult reconcileOnce();

    class RefundingPaymentReconciliationResult {
        private final int tenantCount;
        private final int scannedOrderCount;
        private final int finalizedOrderCount;
        private final int errorCount;

        public RefundingPaymentReconciliationResult(
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
