package com.evcs.payment.dto;

import com.evcs.payment.dto.ReconciliationExceptionItem.ExceptionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账异常候选信息
 *
 * 用于在对账比对阶段收集原始上下文，再由异常服务进行统一检测与分级。
 */
@Data
@Builder
public class ReconciliationExceptionCandidate {

    /**
     * 异常类型
     */
    private ExceptionType type;

    /**
     * 描述信息
     */
    private String description;

    /**
     * 系统侧交易号
     */
    private String systemTradeNo;

    /**
     * 渠道侧交易号
     */
    private String channelTradeNo;

    /**
     * 系统交易金额
     */
    private BigDecimal systemAmount;

    /**
     * 渠道交易金额
     */
    private BigDecimal channelAmount;

    /**
     * 金额差异
     */
    private BigDecimal amountDifference;

    /**
     * 系统记录的交易时间
     */
    private LocalDateTime systemTradeTime;

    /**
     * 渠道记录的交易时间
     */
    private LocalDateTime channelTradeTime;

    /**
     * 系统记录的交易状态
     */
    private String systemStatus;

    /**
     * 渠道记录的交易状态
     */
    private String channelStatus;

    /**
     * 附加备注
     */
    private String remark;
}
