package com.evcs.payment.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 对账请求
 */
@Data
public class ReconciliationRequest {
    /**
     * 对账日期
     */
    private LocalDate reconciliationDate;

    /**
     * 支付渠道（alipay/wechat）
     */
    private String channel;
}
