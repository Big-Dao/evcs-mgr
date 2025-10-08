package com.evcs.order.service;

public interface ReconciliationService {
    ReconcileResult runDailyCheck();

    class ReconcileResult {
        public long total;
        public long invalidTimeRange;
        public long negativeAmount;
        public long missingEndTime;
        public long needAttention;
    }
}
