package com.evcs.payment.service.reconciliation.impl;

import com.evcs.payment.dto.ReconciliationExceptionItem;
import com.evcs.payment.dto.ReconciliationExceptionCandidate;
import com.evcs.payment.service.reconciliation.ReconciliationExceptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对账异常处理服务实现
 */
@Slf4j
@Service
public class ReconciliationExceptionServiceImpl implements ReconciliationExceptionService {

    /**
     * 最近一次检测到的异常缓存，方便生成报告或复查
     */
    private final Map<String, List<ReconciliationExceptionItem>> latestExceptionCache = new ConcurrentHashMap<>();

    @Override
    public List<ReconciliationExceptionItem> detectExceptions(String reconciliationId,
                                                          List<ReconciliationExceptionCandidate> candidates) {
        log.info("检测对账异常: reconciliationId={}, candidateCount={}",
            reconciliationId, candidates == null ? 0 : candidates.size());

        if (candidates == null || candidates.isEmpty()) {
            latestExceptionCache.remove(reconciliationId);
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        List<ReconciliationExceptionItem> exceptions = new ArrayList<>(candidates.size());

        for (ReconciliationExceptionCandidate candidate : candidates) {
            ReconciliationExceptionItem exception = buildExceptionFromCandidate(reconciliationId, candidate, now);
            exceptions.add(exception);
        }

        latestExceptionCache.put(reconciliationId, exceptions);
        log.info("检测到对账异常: reconciliationId={}, count={}", reconciliationId, exceptions.size());
        return exceptions;
    }

    @Override
    public boolean handleException(ReconciliationExceptionItem exception) {
        log.info("处理对账异常: exceptionId={}, type={}", exception.getId(), exception.getType());

        try {
            exception.setStatus(ReconciliationExceptionItem.ExceptionStatus.PROCESSING);
            exception.setUpdateTime(LocalDateTime.now());

            String handleRemark;
            switch (exception.getType()) {
                case TRADE_NOT_FOUND:
                    handleRemark = handleTradeNotFoundException(exception);
                    break;
                case AMOUNT_MISMATCH:
                    handleRemark = handleAmountMismatchException(exception);
                    break;
                case STATUS_MISMATCH:
                    handleRemark = handleStatusMismatchException(exception);
                    break;
                default:
                    handleRemark = handleGenericException(exception);
                    break;
            }

            // 标记为已解决
            exception.setStatus(ReconciliationExceptionItem.ExceptionStatus.RESOLVED);
            exception.setHandleTime(LocalDateTime.now());
            exception.setHandleRemark(handleRemark);
            exception.setUpdateTime(LocalDateTime.now());

            log.info("异常处理完成: exceptionId={}", exception.getId());
            return true;

        } catch (Exception e) {
            log.error("处理对账异常失败: exceptionId={}", exception.getId(), e);
            exception.setStatus(ReconciliationExceptionItem.ExceptionStatus.PENDING);
            exception.setHandleRemark("处理失败: " + e.getMessage());
            exception.setUpdateTime(LocalDateTime.now());
            return false;
        }
    }

    @Override
    public ReconciliationExceptionHandleResult handleExceptions(List<ReconciliationExceptionItem> exceptions) {
        log.info("批量处理对账异常: count={}", exceptions.size());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String> errors = new ArrayList<>();

        for (ReconciliationExceptionItem exception : exceptions) {
            try {
                boolean success = handleException(exception);
                if (success) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                    errors.add("处理异常失败: " + exception.getId());
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
                errors.add("处理异常异常: " + exception.getId() + " - " + e.getMessage());
            }
        }

        log.info("批量处理完成: total={}, success={}, failure={}",
                exceptions.size(), successCount.get(), failureCount.get());

        return new ReconciliationExceptionHandleResult(
                exceptions.size(), successCount.get(), failureCount.get(), errors);
    }

    @Override
    public String generateExceptionReport(String reconciliationId) {
        log.info("生成异常报告: reconciliationId={}", reconciliationId);

        try {
            List<ReconciliationExceptionItem> exceptions = latestExceptionCache.getOrDefault(
                reconciliationId, Collections.emptyList());
            if (exceptions.isEmpty()) {
                log.warn("未找到异常缓存: reconciliationId={}", reconciliationId);
                return "当前对账任务无可用的异常记录";
            }

            StringBuilder report = new StringBuilder();
            report.append("对账异常报告\n");
            report.append("=====================================\n");
            report.append("对账ID: ").append(reconciliationId).append("\n");
            report.append("生成时间: ").append(LocalDateTime.now()).append("\n");
            report.append("异常总数: ").append(exceptions.size()).append("\n\n");

            // 按类型分组统计
            report.append("异常类型统计:\n");
            for (ReconciliationExceptionItem.ExceptionType type : ReconciliationExceptionItem.ExceptionType.values()) {
                long count = exceptions.stream()
                    .filter(e -> e.getType() == type)
                    .count();
                if (count > 0) {
                    report.append("- ").append(type.getDescription()).append(": ").append(count).append("\n");
                }
            }

            // 按级别分组统计
            report.append("\n异常级别统计:\n");
            for (ReconciliationExceptionItem.ExceptionLevel level : ReconciliationExceptionItem.ExceptionLevel.values()) {
                long count = exceptions.stream()
                    .filter(e -> e.getLevel() == level)
                    .count();
                if (count > 0) {
                    report.append("- ").append(level.getDescription()).append(": ").append(count).append("\n");
                }
            }

            // 异常详情
            report.append("\n异常详情:\n");
            for (int i = 0; i < exceptions.size(); i++) {
                ReconciliationExceptionItem exception = exceptions.get(i);
                report.append(i + 1).append(". ").append(exception.getType().getDescription())
                    .append(" (").append(exception.getLevel().getDescription()).append(")\n");
                report.append("   系统交易号: ").append(exception.getSystemTradeNo()).append("\n");
                report.append("   渠道交易号: ").append(exception.getChannelTradeNo()).append("\n");
                report.append("   金额差异: ").append(exception.getAmountDifference()).append("\n");
                report.append("   描述: ").append(exception.getDescription()).append("\n\n");
            }

            return report.toString();

        } catch (Exception e) {
            log.error("生成异常报告失败: reconciliationId={}", reconciliationId, e);
            return "生成异常报告失败: " + e.getMessage();
        }
    }

    private ReconciliationExceptionItem buildExceptionFromCandidate(String reconciliationId,
                                                                ReconciliationExceptionCandidate candidate,
                                                                LocalDateTime now) {
        BigDecimal amountDifference = candidate.getAmountDifference();
        if (amountDifference != null) {
            amountDifference = amountDifference.abs();
        }

        ReconciliationExceptionItem.ExceptionLevel level = determineExceptionLevel(candidate, amountDifference);

        return ReconciliationExceptionItem.builder()
            .id(UUID.randomUUID().toString())
            .reconciliationId(reconciliationId)
            .type(candidate.getType())
            .description(candidate.getDescription())
            .systemTradeNo(candidate.getSystemTradeNo())
            .channelTradeNo(candidate.getChannelTradeNo())
            .systemAmount(candidate.getSystemAmount())
            .channelAmount(candidate.getChannelAmount())
            .amountDifference(amountDifference)
            .systemStatus(candidate.getSystemStatus())
            .channelStatus(candidate.getChannelStatus())
            .level(level)
            .status(ReconciliationExceptionItem.ExceptionStatus.PENDING)
            .createTime(now)
            .updateTime(now)
            .build();
    }

    private ReconciliationExceptionItem.ExceptionLevel determineExceptionLevel(
            ReconciliationExceptionCandidate candidate, BigDecimal amountDifference) {
        if (candidate.getType() == ReconciliationExceptionItem.ExceptionType.TRADE_NOT_FOUND
            || candidate.getType() == ReconciliationExceptionItem.ExceptionType.DUPLICATE_TRADE) {
            return ReconciliationExceptionItem.ExceptionLevel.HIGH;
        }

        if (candidate.getType() == ReconciliationExceptionItem.ExceptionType.AMOUNT_MISMATCH) {
            if (amountDifference != null) {
                if (amountDifference.compareTo(new BigDecimal("5")) > 0) {
                    return ReconciliationExceptionItem.ExceptionLevel.HIGH;
                }
                if (amountDifference.compareTo(new BigDecimal("1")) > 0) {
                    return ReconciliationExceptionItem.ExceptionLevel.MEDIUM;
                }
            }
            return ReconciliationExceptionItem.ExceptionLevel.LOW;
        }

        if (candidate.getType() == ReconciliationExceptionItem.ExceptionType.STATUS_MISMATCH) {
            return ReconciliationExceptionItem.ExceptionLevel.MEDIUM;
        }

        if (candidate.getType() == ReconciliationExceptionItem.ExceptionType.TRADE_TIME_MISMATCH) {
            return ReconciliationExceptionItem.ExceptionLevel.LOW;
        }

        return ReconciliationExceptionItem.ExceptionLevel.LOW;
    }

    private String handleTradeNotFoundException(ReconciliationExceptionItem exception) {
        String remark;
        if (exception.getChannelTradeNo() != null && exception.getSystemTradeNo() == null) {
            remark = "渠道存在交易但系统缺失，已触发补录与人工复核流程";
        } else if (exception.getSystemTradeNo() != null && exception.getChannelTradeNo() == null) {
            remark = "系统订单存在但渠道缺失，已提交渠道确认任务";
        } else {
            remark = "交易缺失，已记录人工复核";
        }
        log.warn("处理交易缺失异常: sys={}, channel={}", exception.getSystemTradeNo(), exception.getChannelTradeNo());
        return remark;
    }

    private String handleAmountMismatchException(ReconciliationExceptionItem exception) {
        BigDecimal diff = exception.getAmountDifference() == null
            ? BigDecimal.ZERO : exception.getAmountDifference().abs();
        String remark;
        if (diff.compareTo(new BigDecimal("0.50")) <= 0) {
            remark = "金额差异<=0.50，判定为手续费或四舍五入，已登记确认";
        } else {
            remark = "金额差异超过阈值，已生成调账任务并通知财务确认";
        }
        log.warn("处理金额不一致异常: tradeNo={}, diff={}",
            exception.getSystemTradeNo(), diff);
        return remark;
    }

    private String handleStatusMismatchException(ReconciliationExceptionItem exception) {
        String systemStatus = normalizeStatus(exception.getSystemStatus());
        String channelStatus = normalizeStatus(exception.getChannelStatus());

        String remark;
        if ("SUCCESS".equals(channelStatus) && !"SUCCESS".equals(systemStatus)) {
            exception.setSystemStatus("SUCCESS");
            remark = "以渠道为准更新系统状态为SUCCESS，等待持久化同步";
        } else if ("FAILED".equals(channelStatus) && "SUCCESS".equals(systemStatus)) {
            remark = "渠道失败系统成功，已触发退款/补偿排查";
        } else {
            remark = "状态不一致，已通知业务补偿组件复核";
        }

        log.warn("处理状态不一致异常: sys={}, channel={}", systemStatus, channelStatus);
        return remark;
    }

    private String handleGenericException(ReconciliationExceptionItem exception) {
        String remark = "异常类型 " + exception.getType().getDescription() + " 已记录等待人工确认";
        log.info("处理通用异常: type={}, remark={}", exception.getType(), remark);
        return remark;
    }

    private String normalizeStatus(String status) {
        return status == null ? null : status.trim().toUpperCase();
    }
}
