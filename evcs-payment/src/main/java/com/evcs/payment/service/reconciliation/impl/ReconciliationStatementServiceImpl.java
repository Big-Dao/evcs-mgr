package com.evcs.payment.service.reconciliation.impl;

import com.evcs.payment.dto.ReconciliationStatement;
import com.evcs.payment.service.reconciliation.AlipayReconciliationService;
import com.evcs.payment.service.reconciliation.ReconciliationStatementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 对账单服务实现
 *
 * 目前为模拟实现，后续集成真实支付渠道API
 */
@Slf4j
@Service
public class ReconciliationStatementServiceImpl implements ReconciliationStatementService {

    @Value("${evcs.payment.reconciliation.mock.enabled:true}")
    private boolean mockEnabled;

    @Resource
    private AlipayReconciliationService alipayReconciliationService;

    @Override
    public ReconciliationStatement downloadStatement(String channel, LocalDate date) {
        log.info("开始下载对账单: channel={}, date={}", channel, date);

        if (mockEnabled) {
            return createMockStatement(channel, date);
        }

        try {
            // 实现真实的对账单下载
            switch (channel.toLowerCase()) {
                case "alipay":
                    return downloadAlipayStatement(date);
                case "wechat":
                    log.warn("微信对账单下载功能待实现，返回模拟数据");
                    return createMockStatement(channel, date);
                default:
                    log.warn("不支持的渠道: {}", channel);
                    return createMockStatement(channel, date);
            }

        } catch (Exception e) {
            log.error("下载对账单失败: channel={}, date={}", channel, date, e);
            return ReconciliationStatement.builder()
                .statementDate(date)
                .channel(channel)
                .status(ReconciliationStatement.StatementStatus.PARSE_FAILED)
                .errorMessage(e.getMessage())
                .downloadTime(LocalDateTime.now())
                .build();
        }
    }

    @Override
    public ReconciliationStatement parseStatement(String channel, String statementData) {
        log.info("开始解析对账单: channel={}, dataSize={}", channel, statementData.length());

        try {
            if (mockEnabled) {
                return parseMockStatement(channel, statementData);
            }

            // TODO: 实现真实的对账单解析
            // 支付宝：解析CSV格式
            // 微信支付：解析CSV或TXT格式

            log.warn("真实对账单解析功能待实现，返回模拟解析结果");
            return parseMockStatement(channel, statementData);

        } catch (Exception e) {
            log.error("解析对账单失败: channel={}", channel, e);
            return ReconciliationStatement.builder()
                .channel(channel)
                .rawData(statementData)
                .status(ReconciliationStatement.StatementStatus.PARSE_FAILED)
                .errorMessage(e.getMessage())
                .parseTime(LocalDateTime.now())
                .build();
        }
    }

    @Override
    public boolean validateStatementFormat(String channel, String statementData) {
        if (statementData == null || statementData.trim().isEmpty()) {
            return false;
        }

        // 基础格式验证
        if (mockEnabled) {
            // 模拟数据格式验证
            return statementData.contains("mock_statement_data");
        }

        // TODO: 实现真实的格式验证
        // 检查文件头、列数、数据格式等

        return true;
    }

    /**
     * 创建模拟对账单
     */
    private ReconciliationStatement createMockStatement(String channel, LocalDate date) {
        List<ReconciliationStatement.StatementTransaction> transactions = new ArrayList<>();

        // 生成模拟交易数据
        for (int i = 0; i < 10; i++) {
            ReconciliationStatement.StatementTransaction transaction = ReconciliationStatement.StatementTransaction.builder()
                .outTradeNo("ORDER_" + date + "_" + String.format("%03d", i + 1))
                .tradeNo(channel.toUpperCase() + "_" + System.currentTimeMillis() + "_" + i)
                .amount(java.math.BigDecimal.valueOf(100.00 + (i * 10)))
                .tradeStatus("TRADE_SUCCESS")
                .tradeTime(LocalDateTime.now().minusHours(i))
                .tradeType("支付")
                .fee(java.math.BigDecimal.valueOf(0.60))
                .refundAmount(null)
                .originalTradeNo(null)
                .remark("模拟交易数据")
                .build();
            transactions.add(transaction);
        }

        java.math.BigDecimal totalAmount = transactions.stream()
            .map(ReconciliationStatement.StatementTransaction::getAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        return ReconciliationStatement.builder()
            .statementDate(date)
            .channel(channel)
            .rawData("mock_statement_data_" + channel + "_" + date)
            .transactions(transactions)
            .totalCount(transactions.size())
            .totalAmount(totalAmount)
            .successCount(transactions.size())
            .successAmount(totalAmount)
            .failureCount(0)
            .failureAmount(java.math.BigDecimal.ZERO)
            .refundCount(0)
            .refundAmount(java.math.BigDecimal.ZERO)
            .status(ReconciliationStatement.StatementStatus.PARSED)
            .downloadTime(LocalDateTime.now())
            .parseTime(LocalDateTime.now())
            .build();
    }

    /**
     * 解析模拟对账单
     */
    private ReconciliationStatement parseMockStatement(String channel, String statementData) {
        // 验证格式
        if (!validateStatementFormat(channel, statementData)) {
            throw new IllegalArgumentException("对账单格式不正确");
        }

        // 从模拟数据中提取日期
        LocalDate date = LocalDate.now().minusDays(1);
        if (statementData.contains("_")) {
            String[] parts = statementData.split("_");
            if (parts.length >= 3) {
                try {
                    date = LocalDate.parse(parts[2], DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception e) {
                    log.warn("解析日期失败，使用默认日期: {}", e.getMessage());
                }
            }
        }

        return createMockStatement(channel, date);
    }

    /**
     * 下载支付宝对账单
     */
    private ReconciliationStatement downloadAlipayStatement(LocalDate date) {
        log.info("开始下载支付宝对账单: date={}", date);

        try {
            // 1. 下载并解析支付宝对账单
            List<AlipayReconciliationService.AlipayBillRecord> billRecords =
                alipayReconciliationService.downloadAndParseBill(date);

            // 2. 转换为对账单对象
            List<ReconciliationStatement.StatementTransaction> transactions =
                billRecords.stream()
                    .map(this::convertAlipayRecord)
                    .collect(java.util.stream.Collectors.toList());

            // 3. 计算统计信息
            long totalCount = transactions.size();
            long successCount = transactions.stream()
                .filter(t -> "TRADE_SUCCESS".equals(t.getTradeStatus()) || "TRADE_FINISHED".equals(t.getTradeStatus()))
                .count();
            long refundCount = transactions.stream()
                .filter(t -> t.getRefundAmount() != null && t.getRefundAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                .count();

            java.math.BigDecimal totalAmount = transactions.stream()
                .filter(t -> t.getAmount() != null)
                .map(ReconciliationStatement.StatementTransaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal refundAmount = transactions.stream()
                .filter(t -> t.getRefundAmount() != null)
                .map(ReconciliationStatement.StatementTransaction::getRefundAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            log.info("支付宝对账单下载成功: date={}, totalCount={}, successCount={}, totalAmount={}",
                date, totalCount, successCount, totalAmount);

            return ReconciliationStatement.builder()
                .statementDate(date)
                .channel("alipay")
                .transactions(transactions)
                .totalCount((int) totalCount)
                .successCount((int) successCount)
                .totalAmount(totalAmount)
                .refundCount((int) refundCount)
                .refundAmount(refundAmount)
                .status(ReconciliationStatement.StatementStatus.PARSED)
                .downloadTime(LocalDateTime.now())
                .parseTime(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("下载支付宝对账单失败: date={}", date, e);
            return ReconciliationStatement.builder()
                .statementDate(date)
                .channel("alipay")
                .status(ReconciliationStatement.StatementStatus.PARSE_FAILED)
                .errorMessage(e.getMessage())
                .downloadTime(LocalDateTime.now())
                .build();
        }
    }

    /**
     * 转换支付宝账单记录为对账单交易记录
     */
    private ReconciliationStatement.StatementTransaction convertAlipayRecord(
            AlipayReconciliationService.AlipayBillRecord record) {

        // 转换交易状态
        String tradeStatus = "UNKNOWN";
        if ("交易支付成功".equals(record.getBusinessType()) || "交易创建".equals(record.getBusinessType())) {
            if (record.getFinishTime() != null && !record.getFinishTime().isEmpty()) {
                tradeStatus = "TRADE_SUCCESS";
            } else {
                tradeStatus = "TRADE_PENDING";
            }
        } else if ("退款".equals(record.getBusinessType())) {
            tradeStatus = "TRADE_REFUND";
        }

        // 转换交易类型
        String tradeType = "支付";
        if ("退款".equals(record.getBusinessType())) {
            tradeType = "退款";
        }

        return ReconciliationStatement.StatementTransaction.builder()
            .outTradeNo(record.getOutTradeNo())
            .tradeNo(record.getTradeNo())
            .amount(record.getTotalAmount())
            .tradeStatus(tradeStatus)
            .tradeTime(parseDateTime(record.getFinishTime()))
            .tradeType(tradeType)
            .fee(record.getServiceFee())
            .refundAmount(record.getRefundAmount())
            .originalTradeNo(null)
            .remark(record.getRemark())
            .build();
    }

    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 支付宝时间格式: 2024-11-02 18:30:25
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr.trim(), formatter);
        } catch (Exception e) {
            log.warn("解析日期时间失败: {}", dateTimeStr, e);
            return null;
        }
    }
}