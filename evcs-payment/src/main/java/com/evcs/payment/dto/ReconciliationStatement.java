package com.evcs.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 对账单数据
 */
@Data
@Builder
public class ReconciliationStatement {

    /**
     * 对账日期
     */
    private LocalDate statementDate;

    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 对账单原始数据
     */
    private String rawData;

    /**
     * 交易记录列表
     */
    private List<StatementTransaction> transactions;

    /**
     * 总交易笔数
     */
    private Integer totalCount;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 成功交易笔数
     */
    private Integer successCount;

    /**
     * 成功交易总金额
     */
    private BigDecimal successAmount;

    /**
     * 失败交易笔数
     */
    private Integer failureCount;

    /**
     * 失败交易总金额
     */
    private BigDecimal failureAmount;

    /**
     * 退款交易笔数
     */
    private Integer refundCount;

    /**
     * 退款交易总金额
     */
    private BigDecimal refundAmount;

    /**
     * 对账单状态
     */
    private StatementStatus status;

    /**
     * 下载时间
     */
    private LocalDateTime downloadTime;

    /**
     * 解析时间
     */
    private LocalDateTime parseTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 对账单条目
     */
    @Data
    @Builder
    public static class StatementTransaction {
        /**
         * 商户订单号
         */
        private String outTradeNo;

        /**
         * 渠道交易号
         */
        private String tradeNo;

        /**
         * 交易金额
         */
        private BigDecimal amount;

        /**
         * 交易状态
         */
        private String tradeStatus;

        /**
         * 交易时间
         */
        private LocalDateTime tradeTime;

        /**
         * 交易类型（支付/退款）
         */
        private String tradeType;

        /**
         * 手续费
         */
        private BigDecimal fee;

        /**
         * 退款金额（如果是退款交易）
         */
        private BigDecimal refundAmount;

        /**
         * 原始交易号（退款时关联原交易）
         */
        private String originalTradeNo;

        /**
         * 备注
         */
        private String remark;
    }

    /**
     * 对账单状态枚举
     */
    public enum StatementStatus {
        DOWNLOADED("已下载"),
        PARSED("已解析"),
        PARSE_FAILED("解析失败"),
        RECONCILED("已对账"),
        RECONCILE_FAILED("对账失败");

        private final String description;

        StatementStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}