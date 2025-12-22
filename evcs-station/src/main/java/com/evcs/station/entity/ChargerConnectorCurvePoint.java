package com.evcs.station.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 充电曲线点（会话维度）
 * 对应 charger_connector_curve_point 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "charger_connector_curve_point")
public class ChargerConnectorCurvePoint extends BaseEntity {

    @TableId(value = "charger_connector_curve_point_id", type = IdType.AUTO)
    private Long id;

    private Long chargerId;

    /**
     * 枪口编号（从 1 开始）
     */
    private Integer connectorNo;

    private String sessionId;

    private LocalDateTime sampleTime;

    private BigDecimal voltage;

    /**
     * 电流（A）
     *
     * 注意：字段名避免使用 SQL 关键字 current，否则 MyBatis-Plus 的租户拦截器解析 SQL 时会失败。
     */
    @JsonProperty("current")
    private BigDecimal currentA;

    /**
     * 功率（kW）
     */
    private BigDecimal power;

    /**
     * SOC（%）
     */
    private BigDecimal soc;

    /**
     * 累计电量（kWh）
     */
    private BigDecimal energy;

    private Long durationSeconds;
}
