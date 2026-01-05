package com.evcs.payment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.payment.dto.ReconciliationQuery;
import com.evcs.payment.dto.ReconciliationRequest;
import com.evcs.payment.dto.ReconciliationResult;
import com.evcs.payment.entity.ReconciliationTask;

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

    /**
     * 分页查询对账任务
     */
    Page<ReconciliationTask> getTaskList(ReconciliationQuery query);

    /**
     * 获取对账任务详情
     */
    ReconciliationTask getTaskDetail(Long id);

    /**
     * 获取对账报告
     */
    Object getReport(String taskNo);
}
