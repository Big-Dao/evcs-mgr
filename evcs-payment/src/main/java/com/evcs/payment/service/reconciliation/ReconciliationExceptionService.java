package com.evcs.payment.service.reconciliation;

import com.evcs.payment.dto.ReconciliationException;

import java.util.List;

/**
 * 对账异常处理服务接口
 *
 * 处理对账过程中发现的异常情况
 */
public interface ReconciliationExceptionService {

    /**
     * 检测对账异常
     *
     * @param reconciliationId 对账ID
     * @return 异常列表
     */
    List<ReconciliationException> detectExceptions(String reconciliationId);

    /**
     * 处理对账异常
     *
     * @param exception 异常信息
     * @return 是否处理成功
     */
    boolean handleException(ReconciliationException exception);

    /**
     * 批量处理异常
     *
     * @param exceptions 异常列表
     * @return 处理结果
     */
    ReconciliationExceptionHandleResult handleExceptions(List<ReconciliationException> exceptions);

    /**
     * 生成异常报告
     *
     * @param reconciliationId 对账ID
     * @return 异常报告
     */
    String generateExceptionReport(String reconciliationId);

    /**
     * 异常处理结果
     */
    class ReconciliationExceptionHandleResult {
        private int totalCount;
        private int successCount;
        private int failureCount;
        private List<String> errors;

        public ReconciliationExceptionHandleResult(int totalCount, int successCount, int failureCount, List<String> errors) {
            this.totalCount = totalCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errors = errors;
        }

        // Getters
        public int getTotalCount() { return totalCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public List<String> getErrors() { return errors; }
        public double getSuccessRate() {
            return totalCount > 0 ? (double) successCount / totalCount * 100 : 0;
        }
    }
}