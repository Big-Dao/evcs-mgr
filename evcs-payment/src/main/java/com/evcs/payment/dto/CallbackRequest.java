package com.evcs.payment.dto;

import lombok.Data;

import java.util.Map;

/**
 * 支付回调请求
 */
@Data
public class CallbackRequest {
    /**
     * 支付渠道
     */
    private String channel;

    /**
     * 交易流水号
     */
    private String tradeNo;

    /**
     * 商户订单号
     */
    private String outTradeNo;

    /**
     * 支付状态
     */
    private String tradeStatus;

    /**
     * 支付金额
     */
    private String totalAmount;

    /**
     * 买家信息
     */
    private String buyerId;

    /**
     * 支付时间
     */
    private String gmtPayment;

    /**
     * 回调原始数据
     */
    private String rawData;

    /**
     * 签名
     */
    private String sign;

    /**
     * 签名类型
     */
    private String signType;

    /**
     * 其他参数
     */
    private Map<String, String> extraParams;
}