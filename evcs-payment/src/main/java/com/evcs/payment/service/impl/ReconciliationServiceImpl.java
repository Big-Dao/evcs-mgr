package com.evcs.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.evcs.common.annotation.DataScope;
import com.evcs.payment.dto.ReconciliationRequest;
import com.evcs.payment.dto.ReconciliationResult;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.service.IReconciliationService;
import com.evcs.payment.service.reconciliation.ReconciliationExceptionService;
import com.evcs.payment.service.reconciliation.ReconciliationStatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 对账服务实现
 * 
 * TODO: 完整实现需要：
 * 1. 集成支付宝对账单下载API
 * 2. 集成微信支付对账单下载API
 * 3. 解析对账单CSV文件
 * 4. 比对系统订单与对账单数据
 * 5. 生成对账报表并导出
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements IReconciliationService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final ReconciliationStatementService statementService;
    private final ReconciliationExceptionService exceptionService;

    @Override
    @DataScope
    public ReconciliationResult reconcile(ReconciliationRequest request) {
        log.info("开始对账: date={}, channel={}", request.getReconciliationDate(), request.getChannel());

        LocalDate date = request.getReconciliationDate();
        LocalDateTime startTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

        // 查询当天的支付订单
        List<PaymentOrder> orders = paymentOrderMapper.selectList(
            new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getStatus, PaymentStatus.SUCCESS.getCode())
                .like(PaymentOrder::getPaymentMethod, request.getChannel())
                .between(PaymentOrder::getCreateTime, startTime, endTime)
        );

        // 计算系统总金额
        BigDecimal systemTotalAmount = orders.stream()
            .map(PaymentOrder::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 下载并解析对账单
        BigDecimal channelTotalAmount;
        int totalCount;
        int matchedCount;
        int mismatchCount;

        try {
            log.info("开始下载对账单: channel={}, date={}", request.getChannel(), date);
            var statement = statementService.downloadStatement(request.getChannel(), date);

            if (statement != null && statement.getTransactions() != null) {
                channelTotalAmount = statement.getTotalAmount() != null ?
                    statement.getTotalAmount() : BigDecimal.ZERO;

                // 执行详细的交易比对
                var reconciliationDetails = compareTransactions(orders, statement.getTransactions());
                totalCount = reconciliationDetails.getTotalCount();
                matchedCount = reconciliationDetails.getMatchedCount();
                mismatchCount = reconciliationDetails.getMismatchCount();

                // 检测并处理异常
                detectAndHandleExceptions(date.toString() + "_" + request.getChannel(),
                    reconciliationDetails.getExceptions());

            } else {
                log.warn("对账单为空，使用系统数据: channel={}", request.getChannel());
                channelTotalAmount = systemTotalAmount;
                totalCount = orders.size();
                matchedCount = orders.size();
                mismatchCount = 0;
            }

        } catch (Exception e) {
            log.error("对账单处理失败，使用系统数据: channel={}", request.getChannel(), e);
            channelTotalAmount = systemTotalAmount;
            totalCount = orders.size();
            matchedCount = orders.size();
            mismatchCount = 0;
        }

        // 计算对账成功率
        double successRate = totalCount > 0 
            ? (double) matchedCount / totalCount * 100 
            : 100.0;

        // 计算金额差异
        BigDecimal amountDifference = systemTotalAmount.subtract(channelTotalAmount).abs();

        // 确定对账状态
        String status;
        if (mismatchCount == 0 && amountDifference.compareTo(BigDecimal.ZERO) == 0) {
            status = "SUCCESS";
        } else if (mismatchCount == totalCount) {
            status = "FAILED";
        } else {
            status = "PARTIAL";
        }

        ReconciliationResult result = ReconciliationResult.builder()
            .reconciliationDate(date)
            .channel(request.getChannel())
            .totalCount(totalCount)
            .matchedCount(matchedCount)
            .mismatchCount(mismatchCount)
            .systemTotalAmount(systemTotalAmount)
            .channelTotalAmount(channelTotalAmount)
            .amountDifference(amountDifference)
            .successRate(BigDecimal.valueOf(successRate).setScale(2, RoundingMode.HALF_UP).doubleValue())
            .status(status)
            .build();

        log.info("对账完成: date={}, channel={}, totalCount={}, matchedCount={}, successRate={}%", 
            date, request.getChannel(), totalCount, matchedCount, result.getSuccessRate());

        return result;
    }

    @Override
    public ReconciliationResult dailyReconciliation(String channel) {
        // 对昨天的订单进行对账
        LocalDate yesterday = LocalDate.now().minusDays(1);
        ReconciliationRequest request = new ReconciliationRequest();
        request.setReconciliationDate(yesterday);
        request.setChannel(channel);
        return reconcile(request);
    }

    /**
     * 比较系统订单与对账单交易
     */
    private ReconciliationDetails compareTransactions(List<PaymentOrder> systemOrders,
                                                     List<com.evcs.payment.dto.ReconciliationStatement.StatementTransaction> statementTransactions) {
        log.info("开始比对交易数据: systemCount={}, statementCount={}",
                systemOrders.size(), statementTransactions.size());

        ReconciliationDetails details = new ReconciliationDetails();
        details.setTotalCount(Math.max(systemOrders.size(), statementTransactions.size()));

        int matched = 0;
        int mismatched = 0;
        List<String> exceptions = new ArrayList<>();

        // 简化的比对逻辑：按金额和状态匹配
        // TODO: 实现更精确的匹配逻辑（交易号、时间等）
        for (PaymentOrder order : systemOrders) {
            boolean found = false;
            for (var transaction : statementTransactions) {
                if (order.getAmount().equals(transaction.getAmount()) &&
                    "TRADE_SUCCESS".equals(transaction.getTradeStatus())) {
                    found = true;
                    matched++;
                    break;
                }
            }
            if (!found) {
                mismatched++;
                exceptions.add("订单未在对账单中找到: " + order.getTradeNo());
            }
        }

        details.setMatchedCount(matched);
        details.setMismatchCount(mismatched);
        details.setExceptions(exceptions);

        log.info("交易比对完成: total={}, matched={}, mismatched={}",
                details.getTotalCount(), matched, mismatched);

        return details;
    }

    /**
     * 检测并处理异常
     */
    private void detectAndHandleExceptions(String reconciliationId, List<String> exceptionMessages) {
        if (exceptionMessages.isEmpty()) {
            return;
        }

        try {
            log.info("检测到对账异常: count={}", exceptionMessages.size());

            // 检测详细异常
            var exceptions = exceptionService.detectExceptions(reconciliationId);

            if (!exceptions.isEmpty()) {
                // 尝试自动处理异常
                var handleResult = exceptionService.handleExceptions(exceptions);
                log.info("异常处理结果: total={}, success={}, successRate={}%",
                        handleResult.getTotalCount(), handleResult.getSuccessCount(),
                        handleResult.getSuccessRate());

                // 生成异常报告
                String report = exceptionService.generateExceptionReport(reconciliationId);
                log.info("异常报告生成完成: reportLength={}", report.length());
            }

        } catch (Exception e) {
            log.error("检测处理对账异常失败: reconciliationId={}", reconciliationId, e);
        }
    }

    /**
     * 对账详情
     */
    private static class ReconciliationDetails {
        private int totalCount;
        private int matchedCount;
        private int mismatchCount;
        private List<String> exceptions;

        // Getters and setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getMatchedCount() { return matchedCount; }
        public void setMatchedCount(int matchedCount) { this.matchedCount = matchedCount; }
        public int getMismatchCount() { return mismatchCount; }
        public void setMismatchCount(int mismatchCount) { this.mismatchCount = mismatchCount; }
        public List<String> getExceptions() { return exceptions; }
        public void setExceptions(List<String> exceptions) { this.exceptions = exceptions; }
    }
}
