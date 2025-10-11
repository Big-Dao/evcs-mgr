package com.evcs.payment.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.evcs.common.entity.BaseEntity;
import com.evcs.payment.enums.PaymentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_order")
public class PaymentOrder extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务订单ID（充电订单ID）
     */
    private Long orderId;

    /**
     * 交易流水号
     */
    private String tradeNo;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付状态
     */
    private Integer status;

    /**
     * 第三方支付流水号
     */
    private String outTradeNo;

    /**
     * 支付完成时间
     */
    private LocalDateTime paidTime;

    /**
     * 幂等键
     */
    private String idempotentKey;

    /**
     * 支付描述
     */
    private String description;

    /**
     * 支付参数（JSON）
     */
    private String payParams;

    /**
     * 支付URL
     */
    private String payUrl;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款时间
     */
    private LocalDateTime refundTime;

    /**
     * 获取支付状态枚举
     */
    public PaymentStatus getStatusEnum() {
        return PaymentStatus.fromCode(this.status);
    }

    /**
     * 设置支付状态枚举
     */
    public void setStatusEnum(PaymentStatus status) {
        this.status = status != null ? status.getCode() : null;
    }
}
