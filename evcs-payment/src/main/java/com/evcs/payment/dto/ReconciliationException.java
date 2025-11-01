package com.evcs.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账异常信息
 */
@Data
@Builder
public class ReconciliationException {

    /**
     * 异常ID
     */
    private String id;

    /**
     * 对账ID
     */
    private String reconciliationId;

    /**
     * 异常类型
     */
    private ExceptionType type;

    /**
     * 异常描述
     */
    private String description;

    /**
     * 系统订单号
     */
    private String systemTradeNo;

    /**
     * 渠道交易号
     */
    private String channelTradeNo;

    /**
     * 系统金额
     */
    private BigDecimal systemAmount;

    /**
     * 渠道金额
     */
    private BigDecimal channelAmount;

    /**
     * 金额差异
     */
    private BigDecimal amountDifference;

    /**
     * 系统状态
     */
    private String systemStatus;

    /**
     * 渠道状态
     */
    private String channelStatus;

    /**
     * 异常级别
     */
    private ExceptionLevel level;

    /**
     * 异常状态
     */
    private ExceptionStatus status;

    /**
     * 处理人
     */
    private String handler;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理备注
     */
    private String handleRemark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 异常类型枚举
     */
    public enum ExceptionType {
        TRADE_NOT_FOUND("交易缺失"),
        AMOUNT_MISMATCH("金额不一致"),
        STATUS_MISMATCH("状态不一致"),
        TRADE_TIME_MISMATCH("交易时间不一致"),
        REFUND_MISMATCH("退款不一致"),
        FEE_MISMATCH("手续费不一致"),
        DUPLICATE_TRADE("重复交易");

        private final String description;

        ExceptionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 异常级别枚举
     */
    public enum ExceptionLevel {
        HIGH("高"),
        MEDIUM("中"),
        LOW("低");

        private final String description;

        ExceptionLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 异常状态枚举
     */
    public enum ExceptionStatus {
        PENDING("待处理"),
        PROCESSING("处理中"),
        RESOLVED("已解决"),
        IGNORED("已忽略");

        private final String description;

        ExceptionStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}