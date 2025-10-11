package com.evcs.payment.enums;

/**
 * 支付状态枚举
 */
public enum PaymentStatus {
    PENDING(0, "待支付"),
    PROCESSING(1, "支付中"),
    SUCCESS(2, "支付成功"),
    FAILED(3, "支付失败"),
    REFUNDING(4, "退款中"),
    REFUNDED(5, "已退款");

    private final Integer code;
    private final String description;

    PaymentStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PaymentStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
