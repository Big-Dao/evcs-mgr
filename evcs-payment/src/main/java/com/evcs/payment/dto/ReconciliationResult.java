package com.evcs.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 对账结果
 */
@Data
@Builder
public class ReconciliationResult {
    /**
     * 对账日期
     */
    private LocalDate reconciliationDate;

    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 总订单数
     */
    private Integer totalCount;

    /**
     * 匹配成功数
     */
    private Integer matchedCount;

    /**
     * 差异订单数
     */
    private Integer mismatchCount;

    /**
     * 系统总金额
     */
    private BigDecimal systemTotalAmount;

    /**
     * 渠道总金额
     */
    private BigDecimal channelTotalAmount;

    /**
     * 金额差异
     */
    private BigDecimal amountDifference;

    /**
     * 对账成功率
     */
    private Double successRate;

    /**
     * 对账状态：SUCCESS/FAILED/PARTIAL
     */
    private String status;
}
