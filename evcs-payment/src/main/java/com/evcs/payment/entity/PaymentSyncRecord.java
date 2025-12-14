package com.evcs.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 支付同步记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_sync_record")
public class PaymentSyncRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付订单ID
     */
    private Long paymentOrderId;

    /**
     * 同步方式 (DIRECT_API, MESSAGE_QUEUE)
     */
    private String syncMethod;

    /**
     * 同步状态 (SUCCESS, FAILED, PENDING)
     */
    private String syncStatus;

    /**
     * 同步时间
     */
    private LocalDateTime syncTime;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最后一次错误信息
     */
    private String lastError;
}
