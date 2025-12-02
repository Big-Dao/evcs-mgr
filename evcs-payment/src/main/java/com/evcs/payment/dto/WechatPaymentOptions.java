package com.evcs.payment.dto;

import lombok.Data;

/**
 * 微信支付专属参数
 */
@Data
public class WechatPaymentOptions {
    /**
     * JSAPI/小程序支付所需的应用ID
     */
    private String appId;

    /**
     * 支付人OpenID（JSAPI必填）
     */
    private String openId;

    /**
     * 终端IP（Native支付推荐）
     */
    private String payerClientIp;

    /**
     * 自定义数据，会在回调中原样返回
     */
    private String attach;

    /**
     * 商品标记，优惠券或代金券时使用
     */
    private String goodsTag;
}
