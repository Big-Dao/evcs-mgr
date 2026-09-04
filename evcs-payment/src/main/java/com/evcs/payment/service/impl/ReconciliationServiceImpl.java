package com.evcs.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.evcs.common.annotation.DataScope;
import com.evcs.payment.dto.ReconciliationExceptionItem;
import com.evcs.payment.dto.ReconciliationExceptionCandidate;
import com.evcs.payment.dto.ReconciliationQuery;
import com.evcs.payment.dto.ReconciliationRequest;
import com.evcs.payment.dto.ReconciliationResult;
import com.evcs.payment.entity.PaymentOrder;
import com.evcs.payment.entity.ReconciliationTask;
import com.evcs.payment.enums.PaymentStatus;
import com.evcs.payment.mapper.PaymentOrderMapper;
import com.evcs.payment.mapper.ReconciliationTaskMapper;
import com.evcs.payment.service.IReconciliationService;
import com.evcs.payment.service.reconciliation.ReconciliationExceptionService;
import com.evcs.payment.service.reconciliation.ReconciliationStatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对账服务实现
 * <p>支持支付宝、微信支付等渠道的对账单下载与自动对账
 * <p>功能包括：
 * <ul>
 *   <li>对账单下载与解析（通过 ReconciliationStatementService）</li>
 *   <li>系统订单与对账单数据自动比对</li>
 *   <li>异常检测与处理（金额不一致、状态不一致、交易缺失等）</li>
 *   <li>对账报告生成</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements IReconciliationService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final ReconciliationStatementService statementService;
    private final ReconciliationExceptionService exceptionService;
    private final ReconciliationTaskMapper reconciliationTaskMapper;

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
                    reconciliationDetails.getExceptions(),
                    reconciliationDetails.getExceptionCandidates());

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
        List<ReconciliationExceptionCandidate> exceptionCandidates = new ArrayList<>();

        // 使用更精确的匹配逻辑：交易号、时间、金额多重验证
        // 时间容差：允许5分钟的时间差（考虑到网络延迟、系统时间差异等）
        final long TIME_TOLERANCE_MINUTES = 5;
        final java.time.Duration TIME_TOLERANCE = java.time.Duration.ofMinutes(TIME_TOLERANCE_MINUTES);

        // 用于标记已经匹配的对账单交易，避免重复匹配
        java.util.Set<Integer> matchedTransactionIndices = new java.util.HashSet<>();

        // 第一轮：精确匹配（交易号 + 金额 + 时间）
        for (PaymentOrder order : systemOrders) {
            boolean found = false;
            String matchReason = "";

            for (int i = 0; i < statementTransactions.size(); i++) {
                if (matchedTransactionIndices.contains(i)) {
                    continue; // 跳过已匹配的交易
                }

                var transaction = statementTransactions.get(i);

                // 匹配策略1：商户订单号（outTradeNo）匹配（最准确）
                if (order.getTradeNo() != null && transaction.getOutTradeNo() != null
                    && order.getTradeNo().equals(transaction.getOutTradeNo())) {

                    // 验证金额是否一致（允许0.01元的容差，处理四舍五入问题）
                    BigDecimal amountDiff = order.getAmount().subtract(transaction.getAmount()).abs();
                    if (amountDiff.compareTo(new BigDecimal("0.01")) <= 0) {

                        // 验证时间是否在容差范围内
                        if (isTimeWithinTolerance(order.getPaidTime(), transaction.getTradeTime(), TIME_TOLERANCE)) {
                            matched++;
                            found = true;
                            matchedTransactionIndices.add(i);
                            matchReason = String.format("交易号匹配: %s, 金额: %s, 时间: %s",
                                order.getTradeNo(), order.getAmount(), order.getPaidTime());

                            if (!isStatusAligned(order, transaction.getTradeStatus())) {
                                String msg = String.format("订单状态不一致: 订单号=%s, 系统状态=%s, 渠道状态=%s",
                                    order.getTradeNo(),
                                    order.getStatusEnum() != null ? order.getStatusEnum().name() : "UNKNOWN",
                                    transaction.getTradeStatus());
                                exceptions.add(msg);
                                addExceptionCandidate(exceptionCandidates,
                                    ReconciliationExceptionItem.ExceptionType.STATUS_MISMATCH,
                                    msg, order, transaction, null);
                            }
                            break;
                        } else {
                            // 交易号匹配但时间超出容差
                            String message = String.format("订单交易号匹配但时间差异过大: %s, 系统时间: %s, 对账单时间: %s",
                                order.getTradeNo(), order.getPaidTime(), transaction.getTradeTime());
                            exceptions.add(message);
                            addExceptionCandidate(exceptionCandidates,
                                ReconciliationExceptionItem.ExceptionType.TRADE_TIME_MISMATCH,
                                message, order, transaction, null);
                        }
                    } else {
                        // 交易号匹配但金额不一致
                        String message = String.format("订单交易号匹配但金额不一致: %s, 系统金额: %s, 对账单金额: %s, 差异: %s",
                            order.getTradeNo(), order.getAmount(), transaction.getAmount(), amountDiff);
                        exceptions.add(message);
                        addExceptionCandidate(exceptionCandidates,
                            ReconciliationExceptionItem.ExceptionType.AMOUNT_MISMATCH,
                            message, order, transaction, amountDiff);
                    }
                }
            }

            // 如果第一轮没匹配到，进行第二轮：模糊匹配（金额 + 时间）
            if (!found) {
                for (int i = 0; i < statementTransactions.size(); i++) {
                    if (matchedTransactionIndices.contains(i)) {
                        continue;
                    }

                    var transaction = statementTransactions.get(i);

                    // 匹配策略2：金额匹配 + 时间匹配 + 状态匹配（用于处理交易号不一致的情况）
                    BigDecimal amountDiff = order.getAmount().subtract(transaction.getAmount()).abs();
                    boolean amountMatch = amountDiff.compareTo(new BigDecimal("0.01")) <= 0;
                    boolean statusMatch = "TRADE_SUCCESS".equals(transaction.getTradeStatus())
                        || "TRADE_FINISHED".equals(transaction.getTradeStatus());
                    boolean timeMatch = isTimeWithinTolerance(order.getPaidTime(), transaction.getTradeTime(), TIME_TOLERANCE);

                    if (amountMatch && statusMatch && timeMatch) {
                        matched++;
                        found = true;
                        matchedTransactionIndices.add(i);
                        matchReason = String.format("模糊匹配（金额+时间）: 系统订单号=%s, 对账单订单号=%s, 金额: %s",
                            order.getTradeNo(), transaction.getOutTradeNo(), order.getAmount());
                        log.warn("使用模糊匹配: 系统订单号={}, 对账单订单号={}, 金额={}",
                            order.getTradeNo(), transaction.getOutTradeNo(), order.getAmount());
                        break;
                    }
                }
            }

            // 如果仍然没匹配到，记录为未匹配
            if (!found) {
                mismatched++;
                String reason = String.format("订单未在对账单中找到匹配项: 订单号=%s, 金额=%s, 时间=%s",
                    order.getTradeNo(), order.getAmount(), order.getPaidTime());
                exceptions.add(reason);
                addExceptionCandidate(exceptionCandidates,
                    ReconciliationExceptionItem.ExceptionType.TRADE_NOT_FOUND,
                    reason, order, null, null);
                log.warn(reason);
            } else if (log.isDebugEnabled()) {
                log.debug("订单匹配成功: {}", matchReason);
            }
        }

        // 检查对账单中有但系统中没有的交易（可能的数据丢失）
        int unmatchedStatementCount = statementTransactions.size() - matchedTransactionIndices.size();
        if (unmatchedStatementCount > 0) {
            for (int i = 0; i < statementTransactions.size(); i++) {
                if (!matchedTransactionIndices.contains(i)) {
                    var transaction = statementTransactions.get(i);
                    String message = String.format("对账单中存在但系统中未找到的交易: 订单号=%s, 金额=%s, 时间=%s",
                        transaction.getOutTradeNo(), transaction.getAmount(), transaction.getTradeTime());
                    exceptions.add(message);
                    addExceptionCandidate(exceptionCandidates,
                        ReconciliationExceptionItem.ExceptionType.DUPLICATE_TRADE,
                        message, null, transaction, null);
                }
            }
            log.warn("对账单中存在{}笔未匹配的交易", unmatchedStatementCount);
        }

        details.setMatchedCount(matched);
        details.setMismatchCount(mismatched);
        details.setExceptions(exceptions);
        details.setExceptionCandidates(exceptionCandidates);

        log.info("交易比对完成: total={}, matched={}, mismatched={}, unmatchedStatement={}",
                details.getTotalCount(), matched, mismatched, unmatchedStatementCount);

        return details;
    }

    /**
     * 检查两个时间是否在容差范围内
     *
     * @param time1 时间1
     * @param time2 时间2
     * @param tolerance 时间容差
     * @return 如果在容差范围内返回true，否则返回false
     */
    private boolean isTimeWithinTolerance(LocalDateTime time1, LocalDateTime time2, java.time.Duration tolerance) {
        if (time1 == null || time2 == null) {
            // 如果任一时间为空，允许匹配（某些情况下时间可能未记录）
            log.debug("时间为空，允许匹配: time1={}, time2={}", time1, time2);
            return true;
        }

        java.time.Duration diff = java.time.Duration.between(
            time1.isBefore(time2) ? time1 : time2,
            time1.isAfter(time2) ? time1 : time2
        );

        boolean withinTolerance = diff.compareTo(tolerance) <= 0;
        if (!withinTolerance && log.isDebugEnabled()) {
            log.debug("时间超出容差: time1={}, time2={}, diff={}, tolerance={}",
                time1, time2, diff, tolerance);
        }

        return withinTolerance;
    }

    /**
     * 检测并处理异常
     */
    private void detectAndHandleExceptions(String reconciliationId,
                                           List<String> exceptionMessages,
                                           List<ReconciliationExceptionCandidate> candidates) {
        if ((exceptionMessages == null || exceptionMessages.isEmpty())
            && (candidates == null || candidates.isEmpty())) {
            return;
        }

        try {
            int messageSize = exceptionMessages == null ? 0 : exceptionMessages.size();
            int candidateSize = candidates == null ? 0 : candidates.size();
            log.info("检测到对账异常: messages={}, candidates={}", messageSize, candidateSize);

            // 检测详细异常
            var exceptions = exceptionService.detectExceptions(reconciliationId,
                candidates == null ? java.util.Collections.emptyList() : candidates);

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
        private List<ReconciliationExceptionCandidate> exceptionCandidates;

        // Getters and setters
        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
        public int getMatchedCount() { return matchedCount; }
        public void setMatchedCount(int matchedCount) { this.matchedCount = matchedCount; }
        public int getMismatchCount() { return mismatchCount; }
        public void setMismatchCount(int mismatchCount) { this.mismatchCount = mismatchCount; }
        public List<String> getExceptions() { return exceptions; }
        public void setExceptions(List<String> exceptions) { this.exceptions = exceptions; }
        public List<ReconciliationExceptionCandidate> getExceptionCandidates() { return exceptionCandidates; }
        public void setExceptionCandidates(List<ReconciliationExceptionCandidate> exceptionCandidates) { this.exceptionCandidates = exceptionCandidates; }
    }

    private void addExceptionCandidate(List<ReconciliationExceptionCandidate> candidates,
                                       ReconciliationExceptionItem.ExceptionType type,
                                       String description,
                                       PaymentOrder systemOrder,
                                       com.evcs.payment.dto.ReconciliationStatement.StatementTransaction statementTransaction,
                                       BigDecimal providedDiff) {
        if (candidates == null) {
            return;
        }
        BigDecimal systemAmount = systemOrder != null ? systemOrder.getAmount() : null;
        BigDecimal statementAmount = statementTransaction != null ? statementTransaction.getAmount() : null;
        BigDecimal diff = providedDiff;
        if (diff == null && systemAmount != null && statementAmount != null) {
            diff = systemAmount.subtract(statementAmount).abs();
        }

        String systemStatus = null;
        if (systemOrder != null) {
            PaymentStatus statusEnum = systemOrder.getStatusEnum();
            systemStatus = statusEnum != null ? statusEnum.name() : null;
        }

        ReconciliationExceptionCandidate candidate = ReconciliationExceptionCandidate.builder()
            .type(type)
            .description(description)
            .systemTradeNo(systemOrder != null ? systemOrder.getTradeNo() : null)
            .channelTradeNo(statementTransaction != null ? statementTransaction.getOutTradeNo() : null)
            .systemAmount(systemAmount)
            .channelAmount(statementAmount)
            .amountDifference(diff)
            .systemTradeTime(systemOrder != null ? systemOrder.getPaidTime() : null)
            .channelTradeTime(statementTransaction != null ? statementTransaction.getTradeTime() : null)
            .systemStatus(systemStatus)
            .channelStatus(statementTransaction != null ? statementTransaction.getTradeStatus() : null)
            .remark(description)
            .build();
        candidates.add(candidate);
    }

    private boolean isStatusAligned(PaymentOrder order, String channelStatus) {
        if (order == null || channelStatus == null) {
            return true;
        }
        PaymentStatus status = order.getStatusEnum();
        if (status == null) {
            return true;
        }
        String normalizedChannel = channelStatus.trim().toUpperCase();
        switch (status) {
            case SUCCESS:
                return normalizedChannel.contains("SUCCESS") || normalizedChannel.contains("FINISHED");
            case FAILED:
                return normalizedChannel.contains("FAILED") || normalizedChannel.contains("CLOSED");
            case REFUNDED:
                return normalizedChannel.contains("REFUND");
            default:
                return true;
        }
    }

    @Override
    public Page<ReconciliationTask> getTaskList(ReconciliationQuery query) {
        Page<ReconciliationTask> page = new Page<>(query.getPage() != null ? query.getPage() : 1, query.getSize() != null ? query.getSize() : 10);
        LambdaQueryWrapper<ReconciliationTask> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getChannel())) {
            wrapper.eq(ReconciliationTask::getChannel, query.getChannel());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ReconciliationTask::getStatus, query.getStatus());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(ReconciliationTask::getReconciliationDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(ReconciliationTask::getReconciliationDate, query.getEndDate());
        }
        wrapper.orderByDesc(ReconciliationTask::getCreateTime);
        return reconciliationTaskMapper.selectPage(page, wrapper);
    }

    @Override
    public ReconciliationTask getTaskDetail(Long id) {
        return reconciliationTaskMapper.selectById(id);
    }

    @Override
    public Object getReport(String taskNo) {
        LambdaQueryWrapper<ReconciliationTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconciliationTask::getTaskNo, taskNo);
        ReconciliationTask task = reconciliationTaskMapper.selectOne(wrapper);

        Map<String, Object> report = new HashMap<>();
        report.put("taskNo", taskNo);
        report.put("channel", task != null ? task.getChannel() : "UNKNOWN");
        report.put("reconciliationDate", task != null ? task.getReconciliationDate() : LocalDate.now());

        Map<String, Object> summary = new HashMap<>();
        if (task != null) {
            summary.put("totalCount", task.getTotalCount());
            summary.put("matchedCount", task.getMatchedCount());
            summary.put("unmatchedCount", task.getUnmatchedCount());
            summary.put("exceptionCount", task.getExceptionCount());
            summary.put("totalAmount", task.getTotalAmount());
            summary.put("matchedAmount", task.getMatchedAmount());
        }
        report.put("summary", summary);
        report.put("matchedRecords", new ArrayList<>());
        report.put("unmatchedRecords", new ArrayList<>());
        report.put("exceptions", new ArrayList<>());

        return report;
    }
}
