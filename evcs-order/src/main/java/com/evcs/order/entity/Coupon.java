package com.evcs.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon")
public class Coupon extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 类型: 1-满减, 2-折扣
     */
    private Integer type;

    /**
     * 面值 (满减金额 或 折扣率0.8)
     */
    private BigDecimal value;

    /**
     * 最低消费金额
     */
    private BigDecimal minAmount;

    /**
     * 有效期开始
     */
    private LocalDateTime startTime;

    /**
     * 有效期结束
     */
    private LocalDateTime endTime;

    /**
     * 状态: 0-未使用, 1-已使用, 2-已过期
     */
    private Integer status;

    /**
     * 关联使用的订单ID
     */
    private Long orderId;
}
