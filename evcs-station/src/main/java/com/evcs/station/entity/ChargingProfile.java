package com.evcs.station.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电策略配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("charging_profile")
public class ChargingProfile extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联充电桩ID
     */
    private Long chargerId;

    /**
     * 枪口号 (0表示整个充电桩)
     */
    private Integer connectorId;

    /**
     * 优先级 (Stack Level)
     */
    private Integer stackLevel;

    /**
     * 策略目的: ChargePointMaxProfile, TxDefaultProfile, TxProfile
     */
    private String purpose;

    /**
     * 策略类型: Absolute, Recurring, Relative
     */
    private String kind;

    /**
     * 功率限制 (kW)
     */
    private BigDecimal limitKw;

    /**
     * 生效开始时间
     */
    private LocalDateTime validFrom;

    /**
     * 生效结束时间
     */
    private LocalDateTime validTo;

    /**
     * 描述
     */
    private String description;
}
