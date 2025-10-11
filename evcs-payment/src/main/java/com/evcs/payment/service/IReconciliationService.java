package com.evcs.payment.service;

import com.evcs.payment.dto.ReconciliationRequest;
import com.evcs.payment.dto.ReconciliationResult;

/**
 * 对账服务接口
 */
public interface IReconciliationService {

    /**
     * 执行对账
     */
    ReconciliationResult reconcile(ReconciliationRequest request);

    /**
     * 每日自动对账
     */
    ReconciliationResult dailyReconciliation(String channel);
}
