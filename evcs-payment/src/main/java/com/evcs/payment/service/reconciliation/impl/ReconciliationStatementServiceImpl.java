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

            // 实现真实的对账单解析
            switch (channel.toLowerCase()) {
                case "alipay":
                    return parseAlipayStatement(statementData);
                case "wechat":
                case "wechatpay":
                    return parseWechatStatement(statementData);
                default:
                    log.warn("不支持的对账单渠道: {}，返回模拟解析结果", channel);
                    return parseMockStatement(channel, statementData);
            }

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

        // 实现真实的格式验证
        switch (channel.toLowerCase()) {
            case "alipay":
                return validateAlipayStatementFormat(statementData);
            case "wechat":
            case "wechatpay":
                return validateWechatStatementFormat(statementData);
            default:
                // 对于未知渠道，进行基础验证
                return statementData.length() > 0 && statementData.contains(",");
        }
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

    /**
     * 解析支付宝对账单
     * 支付宝对账单格式：CSV格式，包含注释行（#开头）和数据行
     */
    private ReconciliationStatement parseAlipayStatement(String statementData) {
        log.info("开始解析支付宝对账单: dataSize={}", statementData.length());

        try {
            // 使用已有的支付宝对账服务解析
            // 先提取日期信息
            LocalDate statementDate = extractDateFromStatement(statementData);
            if (statementDate == null) {
                statementDate = LocalDate.now().minusDays(1); // 默认使用昨天
            }

            // 解析对账单内容
            List<AlipayReconciliationService.AlipayBillRecord> billRecords =
                parseAlipayBillContent(statementData);

            // 转换为对账单对象
            List<ReconciliationStatement.StatementTransaction> transactions =
                billRecords.stream()
                    .map(this::convertAlipayRecord)
                    .collect(java.util.stream.Collectors.toList());

            // 计算统计信息
            long totalCount = transactions.size();
            long successCount = transactions.stream()
                .filter(t -> "TRADE_SUCCESS".equals(t.getTradeStatus())
                    || "TRADE_FINISHED".equals(t.getTradeStatus()))
                .count();
            long refundCount = transactions.stream()
                .filter(t -> t.getRefundAmount() != null
                    && t.getRefundAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                .count();

            java.math.BigDecimal totalAmount = transactions.stream()
                .filter(t -> t.getAmount() != null)
                .map(ReconciliationStatement.StatementTransaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal refundAmount = transactions.stream()
                .filter(t -> t.getRefundAmount() != null)
                .map(ReconciliationStatement.StatementTransaction::getRefundAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            log.info("支付宝对账单解析成功: date={}, totalCount={}, successCount={}, totalAmount={}",
                statementDate, totalCount, successCount, totalAmount);

            return ReconciliationStatement.builder()
                .statementDate(statementDate)
                .channel("alipay")
                .rawData(statementData)
                .transactions(transactions)
                .totalCount((int) totalCount)
                .successCount((int) successCount)
                .totalAmount(totalAmount)
                .refundCount((int) refundCount)
                .refundAmount(refundAmount)
                .status(ReconciliationStatement.StatementStatus.PARSED)
                .parseTime(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("解析支付宝对账单失败", e);
            throw new RuntimeException("解析支付宝对账单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析支付宝对账单内容（复用AlipayReconciliationService的解析逻辑）
     */
    private List<AlipayReconciliationService.AlipayBillRecord> parseAlipayBillContent(String billContent) {
        List<AlipayReconciliationService.AlipayBillRecord> records = new ArrayList<>();
        String[] lines = billContent.split("\n");

        boolean dataStart = false;
        for (String line : lines) {
            line = line.trim();

            // 跳过注释行和空行
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("#")) {
                if (!dataStart && line.contains(",")) {
                    dataStart = true;
                }
                continue;
            }

            // 检查是否是数据开始行（包含表头）
            if (!dataStart) {
                if (line.contains("商户订单号")) {
                    dataStart = true;
                    continue; // 跳过表头行
                }
                continue;
            }

            try {
                AlipayReconciliationService.AlipayBillRecord record = parseAlipayBillLine(line);
                if (record != null) {
                    records.add(record);
                }
            } catch (Exception e) {
                log.warn("解析对账单行失败: line={}", line, e);
            }
        }

        return records;
    }

    /**
     * 解析单行支付宝对账单数据
     */
    private AlipayReconciliationService.AlipayBillRecord parseAlipayBillLine(String line) {
        String[] fields = line.split(",", -1); // -1保留空字段

        if (fields.length < 24) {
            log.warn("对账单字段数量不足: expected>=24, actual={}, line={}", fields.length, line);
            return null;
        }

        try {
            AlipayReconciliationService.AlipayBillRecord record =
                new AlipayReconciliationService.AlipayBillRecord();
            record.setOutTradeNo(fields[0].trim());           // 商户订单号
            record.setTradeNo(fields[1].trim());              // 支付宝交易号
            record.setBusinessType(fields[2].trim());         // 业务类型
            record.setGoodsName(fields[3].trim());            // 商品名称
            record.setCreateTime(fields[4].trim());           // 创建时间
            record.setFinishTime(fields[5].trim());           // 完成时间
            record.setStoreId(fields[6].trim());              // 门店编号
            record.setStoreName(fields[7].trim());            // 门店名称
            record.setOperator(fields[8].trim());             // 操作员
            record.setTerminalId(fields[9].trim());           // 终端号
            record.setOtherAccount(fields[10].trim());        // 对方账号
            record.setTotalAmount(parseAmount(fields[11]));   // 订单金额

            if (fields.length > 12) {
                record.setMerchantRedAmount(parseAmount(fields[12])); // 商家红包
            }
            if (fields.length > 24) {
                record.setRefundAmount(parseAmount(fields[24]));      // 退款金额
            }
            if (fields.length > 25) {
                record.setServiceFee(parseAmount(fields[25]));        // 资金服务费
            }
            if (fields.length > 27) {
                record.setRemark(fields[27].trim());          // 备注
            }

            return record;
        } catch (Exception e) {
            log.error("解析对账单行失败: line={}", line, e);
            return null;
        }
    }

    /**
     * 解析微信支付对账单
     * 微信支付对账单格式：CSV或TXT格式
     */
    private ReconciliationStatement parseWechatStatement(String statementData) {
        log.info("开始解析微信支付对账单: dataSize={}", statementData.length());

        try {
            // 提取日期信息
            LocalDate statementDate = extractDateFromStatement(statementData);
            if (statementDate == null) {
                statementDate = LocalDate.now().minusDays(1);
            }

            List<ReconciliationStatement.StatementTransaction> transactions =
                parseWechatBillContent(statementData);

            // 计算统计信息
            long totalCount = transactions.size();
            long successCount = transactions.stream()
                .filter(t -> "SUCCESS".equals(t.getTradeStatus())
                    || "支付成功".equals(t.getTradeStatus()))
                .count();
            long refundCount = transactions.stream()
                .filter(t -> t.getRefundAmount() != null
                    && t.getRefundAmount().compareTo(java.math.BigDecimal.ZERO) > 0)
                .count();

            java.math.BigDecimal totalAmount = transactions.stream()
                .filter(t -> t.getAmount() != null)
                .map(ReconciliationStatement.StatementTransaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            java.math.BigDecimal refundAmount = transactions.stream()
                .filter(t -> t.getRefundAmount() != null)
                .map(ReconciliationStatement.StatementTransaction::getRefundAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            log.info("微信支付对账单解析成功: date={}, totalCount={}, successCount={}, totalAmount={}",
                statementDate, totalCount, successCount, totalAmount);

            return ReconciliationStatement.builder()
                .statementDate(statementDate)
                .channel("wechat")
                .rawData(statementData)
                .transactions(transactions)
                .totalCount((int) totalCount)
                .successCount((int) successCount)
                .totalAmount(totalAmount)
                .refundCount((int) refundCount)
                .refundAmount(refundAmount)
                .status(ReconciliationStatement.StatementStatus.PARSED)
                .parseTime(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("解析微信支付对账单失败", e);
            throw new RuntimeException("解析微信支付对账单失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析微信支付对账单内容
     * 微信支付对账单格式参考：交易时间,公众账号ID,商户号,子商户号,设备号,微信订单号,商户订单号,用户标识,交易类型,交易状态,
     * 付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,
     * 商品名称,商户数据包,手续费,费率,订单金额,申请退款金额,费率备注
     */
    private List<ReconciliationStatement.StatementTransaction> parseWechatBillContent(String billContent) {
        List<ReconciliationStatement.StatementTransaction> transactions = new ArrayList<>();
        String[] lines = billContent.split("\n");

        boolean dataStart = false;
        for (String line : lines) {
            line = line.trim();

            // 跳过注释行和空行
            if (line.isEmpty()) {
                continue;
            }

            // 检查是否是数据开始行（包含表头）
            if (!dataStart) {
                if (line.contains("交易时间") || line.contains("微信订单号")) {
                    dataStart = true;
                    continue; // 跳过表头行
                }
                continue;
            }

            try {
                ReconciliationStatement.StatementTransaction transaction = parseWechatBillLine(line);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            } catch (Exception e) {
                log.warn("解析微信对账单行失败: line={}", line, e);
            }
        }

        return transactions;
    }

    /**
     * 解析单行微信支付对账单数据
     */
    private ReconciliationStatement.StatementTransaction parseWechatBillLine(String line) {
        String[] fields = line.split(",", -1); // -1保留空字段

        if (fields.length < 10) {
            log.warn("微信对账单字段数量不足: expected>=10, actual={}, line={}", fields.length, line);
            return null;
        }

        try {
            // 微信支付对账单字段映射（根据实际格式调整）
            String tradeTimeStr = fields.length > 0 ? fields[0].trim() : "";      // 交易时间
            String wechatTradeNo = fields.length > 5 ? fields[5].trim() : "";     // 微信订单号
            String outTradeNo = fields.length > 6 ? fields[6].trim() : "";        // 商户订单号
            String tradeType = fields.length > 8 ? fields[8].trim() : "";         // 交易类型
            String tradeStatus = fields.length > 9 ? fields[9].trim() : "";       // 交易状态
            String totalFee = fields.length > 24 ? fields[24].trim() : "";        // 订单金额（单位：分）
            String refundFee = fields.length > 16 ? fields[16].trim() : "";       // 退款金额（单位：分）

            // 转换金额（微信支付金额单位为分）
            java.math.BigDecimal amount = java.math.BigDecimal.ZERO;
            if (!totalFee.isEmpty()) {
                try {
                    // 将分转换为元
                    amount = new java.math.BigDecimal(totalFee).divide(
                        new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                } catch (NumberFormatException e) {
                    log.warn("解析微信订单金额失败: {}", totalFee);
                }
            }

            java.math.BigDecimal refundAmount = java.math.BigDecimal.ZERO;
            if (!refundFee.isEmpty() && !"0".equals(refundFee)) {
                try {
                    refundAmount = new java.math.BigDecimal(refundFee).divide(
                        new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                } catch (NumberFormatException e) {
                    log.warn("解析微信退款金额失败: {}", refundFee);
                }
            }

            // 解析交易时间（微信支付格式：2018-03-05 12:00:00）
            LocalDateTime tradeTime = parseWechatDateTime(tradeTimeStr);

            // 转换交易状态
            String normalizedStatus = normalizeWechatTradeStatus(tradeStatus);

            return ReconciliationStatement.StatementTransaction.builder()
                .outTradeNo(outTradeNo)
                .tradeNo(wechatTradeNo)
                .amount(amount)
                .tradeStatus(normalizedStatus)
                .tradeTime(tradeTime)
                .tradeType(tradeType)
                .refundAmount(refundAmount.compareTo(java.math.BigDecimal.ZERO) > 0 ? refundAmount : null)
                .remark("")
                .build();

        } catch (Exception e) {
            log.error("解析微信对账单行失败: line={}", line, e);
            return null;
        }
    }

    /**
     * 验证支付宝对账单格式
     */
    private boolean validateAlipayStatementFormat(String statementData) {
        if (statementData == null || statementData.trim().isEmpty()) {
            return false;
        }

        // 检查是否包含支付宝对账单标识
        String data = statementData.trim();
        if (!data.contains("支付宝") && !data.contains("alipay")) {
            // 可能不包含中文标识，继续检查
        }

        // 检查是否包含必要的字段标识
        boolean hasHeader = data.contains("商户订单号") || data.contains("outTradeNo");
        boolean hasData = data.contains(",") && data.split("\n").length > 1;

        if (!hasHeader && !hasData) {
            log.warn("支付宝对账单格式验证失败: 缺少表头或数据");
            return false;
        }

        // 检查是否包含注释行（支付宝对账单通常以#开头）
        boolean hasComments = data.contains("#");
        if (!hasComments && !hasHeader) {
            log.warn("支付宝对账单格式验证失败: 缺少注释行或表头");
            return false;
        }

        return true;
    }

    /**
     * 验证微信支付对账单格式
     */
    private boolean validateWechatStatementFormat(String statementData) {
        if (statementData == null || statementData.trim().isEmpty()) {
            return false;
        }

        String data = statementData.trim();

        // 检查是否包含必要的字段标识
        boolean hasHeader = data.contains("交易时间") || data.contains("微信订单号")
            || data.contains("商户订单号") || data.contains("out_trade_no");
        boolean hasData = data.contains(",") && data.split("\n").length > 1;

        if (!hasHeader && !hasData) {
            log.warn("微信支付对账单格式验证失败: 缺少表头或数据");
            return false;
        }

        return true;
    }

    /**
     * 从对账单中提取日期信息
     */
    private LocalDate extractDateFromStatement(String statementData) {
        if (statementData == null || statementData.isEmpty()) {
            return null;
        }

        // 尝试从注释行中提取日期（支付宝格式：账单时间：2024-11-02）
        String[] lines = statementData.split("\n");
        for (String line : lines) {
            if (line.contains("账单时间") || line.contains("bill_date") || line.contains("billDate")) {
                // 提取日期
                String[] parts = line.split("[:：]");
                if (parts.length >= 2) {
                    try {
                        String dateStr = parts[1].trim();
                        // 尝试多种日期格式
                        LocalDate date = LocalDate.parse(dateStr,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        return date;
                    } catch (Exception e) {
                        log.debug("从对账单中提取日期失败: {}", line);
                    }
                }
            }
        }

        return null;
    }

    /**
     * 解析金额字段
     */
    private java.math.BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            return new java.math.BigDecimal(amountStr.trim());
        } catch (NumberFormatException e) {
            log.warn("金额格式错误: {}", amountStr);
            return java.math.BigDecimal.ZERO;
        }
    }

    /**
     * 解析微信支付日期时间
     */
    private LocalDateTime parseWechatDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 微信支付时间格式: 2018-03-05 12:00:00
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(dateTimeStr.trim(), formatter);
        } catch (Exception e) {
            log.warn("解析微信支付日期时间失败: {}", dateTimeStr, e);
            return null;
        }
    }

    /**
     * 标准化微信支付交易状态
     */
    private String normalizeWechatTradeStatus(String tradeStatus) {
        if (tradeStatus == null || tradeStatus.trim().isEmpty()) {
            return "UNKNOWN";
        }

        String status = tradeStatus.trim();
        // 微信支付状态映射
        if ("SUCCESS".equals(status) || "支付成功".equals(status)) {
            return "TRADE_SUCCESS";
        } else if ("REFUND".equals(status) || "退款".equals(status)) {
            return "TRADE_REFUND";
        } else if ("CLOSED".equals(status) || "已关闭".equals(status)) {
            return "TRADE_CLOSED";
        } else if ("NOTPAY".equals(status) || "未支付".equals(status)) {
            return "TRADE_PENDING";
        } else {
            return status; // 返回原始状态
        }
    }
}