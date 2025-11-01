package com.evcs.payment.service.reconciliation.impl;

import com.evcs.payment.dto.ReconciliationStatement;
import com.evcs.payment.service.reconciliation.ReconciliationStatementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Override
    public ReconciliationStatement downloadStatement(String channel, LocalDate date) {
        log.info("开始下载对账单: channel={}, date={}", channel, date);

        if (mockEnabled) {
            return createMockStatement(channel, date);
        }

        try {
            // TODO: 实现真实的对账单下载
            // 支付宝：调用 alipay.data.dataservice.bill.downloadurl.query
            // 微信支付：调用下载账单API

            log.warn("真实对账单下载功能待实现，返回模拟数据");
            return createMockStatement(channel, date);

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
}