package com.evcs.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.common.tenant.TenantContext;
import com.evcs.order.entity.ChargingOrder;
import com.evcs.order.mapper.ChargingOrderMapper;
import com.evcs.order.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账服务实现
 * PERF-01: 使用分页处理避免OOM
 * 
 * @author EVCS Team
 * @since M4 - Week 4 Performance Optimization
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {
    private final ChargingOrderMapper orderMapper;
    
    private static final int BATCH_SIZE = 1000;

    @Override
    @DataScope
    public ReconcileResult runDailyCheck() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(1).atStartOfDay();
        LocalDateTime end = today.atStartOfDay();
        
        log.info("Starting daily reconciliation check from {} to {}", start, end);
        
        ReconcileResult result = new ReconcileResult();
        long currentPage = 1;
        long totalPages = 0;
        long processedCount = 0;
        
        try {
            // 使用分页查询处理订单，避免一次性加载所有数据导致OOM
            // MyBatis Plus自动添加tenant_id过滤
            do {
                Page<ChargingOrder> page = new Page<>(currentPage, BATCH_SIZE);
                QueryWrapper<ChargingOrder> qw = new QueryWrapper<ChargingOrder>()
                        .ge("create_time", start)
                        .lt("create_time", end)
                        .orderByAsc("id"); // 保证顺序一致性
                
                Page<ChargingOrder> pageResult = orderMapper.selectPage(page, qw);
                totalPages = pageResult.getPages();
                
                // 流式处理当前批次
                for (ChargingOrder order : pageResult.getRecords()) {
                    processOrder(order, result);
                    processedCount++;
                }
                
                // 输出进度日志
                if (currentPage % 10 == 0 || currentPage == totalPages) {
                    log.info("Reconciliation progress: page {}/{}, processed {} orders, found {} issues", 
                            currentPage, totalPages, processedCount, result.needAttention);
                }
                
                currentPage++;
            } while (currentPage <= totalPages);
            
            result.total = processedCount;
            
            log.info("Daily reconciliation completed: total={}, invalidTimeRange={}, missingEndTime={}, negativeAmount={}, needAttention={}", 
                    result.total, result.invalidTimeRange, result.missingEndTime, result.negativeAmount, result.needAttention);
            
        } catch (Exception e) {
            log.error("Reconciliation failed at page {}/{}", currentPage, totalPages, e);
            throw new RuntimeException("Reconciliation failed", e);
        }
        
        return result;
    }
    
    /**
     * 处理单个订单的对账逻辑
     */
    private void processOrder(ChargingOrder order, ReconcileResult result) {
        boolean hasIssue = false;
        
        // 检查时间范围有效性
        if (order.getStartTime() != null && order.getEndTime() != null 
                && !order.getEndTime().isAfter(order.getStartTime())) {
            result.invalidTimeRange++;
            hasIssue = true;
        }
        
        // 检查已完成订单是否有结束时间
        if (order.getEndTime() == null && (order.getStatus() != null 
                && order.getStatus() >= ChargingOrder.STATUS_COMPLETED)) {
            result.missingEndTime++;
            hasIssue = true;
        }
        
        // 检查金额是否为负
        if (order.getAmount() != null && order.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.negativeAmount++;
            hasIssue = true;
        }
        
        if (hasIssue) {
            result.needAttention++;
        }
    }
}
