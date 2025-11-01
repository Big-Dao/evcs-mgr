package com.evcs.payment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 退款回调请求
 */
@Data
public class RefundCallbackRequest {

    /**
     * 渠道名称（alipay/wechat）
     */
    private String channel;

    /**
     * 原支付订单号
     */
    private String outTradeNo;

    /**
     * 退款单号
     */
    private String outRequestNo;

    /**
     * 支付宝交易号
     */
    private String tradeNo;

    /**
     * 退款金额
     */
    private BigDecimal refundFee;

    /**
     * 退款状态
     */
    private String refundStatus;

    /**
     * 退款原因
     */
    private String reason;

    /**
     * 回调时间
     */
    private String gmtRefundPay;

    /**
     * 原始参数（用于调试）
     */
    private Map<String, String> rawParams;

    /**
     * 签名
     */
    private String sign;

    /**
     * 签名类型
     */
    private String signType;
}