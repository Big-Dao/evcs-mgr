package com.evcs.payment.enums;

/**
 * 支付方式枚举
 */
public enum PaymentMethod {
    ALIPAY_APP("alipay_app", "支付宝APP支付"),
    ALIPAY_QR("alipay_qr", "支付宝扫码支付"),
    WECHAT_JSAPI("wechat_jsapi", "微信小程序支付"),
    WECHAT_NATIVE("wechat_native", "微信扫码支付");

    private final String code;
    private final String description;

    PaymentMethod(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
