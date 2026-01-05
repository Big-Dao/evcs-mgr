package com.evcs.station.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.evcs.common.entity.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 充电枪口（Connector）实体
 * 对应 charger_connector 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "charger_connector")
public class ChargerConnector extends BaseEntity {

    @TableId(value = "charger_connector_id", type = IdType.AUTO)
    private Long id;

    /**
     * 充电桩ID
     */
    private Long chargerId;

    /**
     * 枪口编号（OCPP connectorId / 云快充枪号），从 1 开始
     */
    private Integer connectorNo;

    /**
     * 枪口类型（可选）：与 Charger.gunTypes 对应
     */
    private String connectorType;

    /**
     * 当前状态：0-离线，1-空闲，2-充电中，3-故障，4-维护，5-预约中
     */
    private Integer status;

    private String faultCode;
    private String faultDescription;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;

    /**
     * 当前充电会话ID（可选）
     */
    private String currentSessionId;

    /**
     * 当前用户ID（可选）
     */
    private Long currentUserId;

    /**
     * 充电开始时间（可选）
     */
    private LocalDateTime chargingStartTime;

    /**
     * 已充电量（kWh，可选）
     */
    private BigDecimal chargedEnergy;

    /**
     * 已充电时长（分钟，可选）
     */
    private Integer chargedDuration;

    /**
     * 最新一次计量上报时间（可选）
     */
    private LocalDateTime lastMeterTime;

    /**
     * 最新电压（V，可选）
     */
    private BigDecimal lastVoltage;

    /**
     * 最新电流（A，可选）
     */
    private BigDecimal lastCurrent;

    /**
     * 最新功率（kW，可选）
     */
    private BigDecimal lastPower;

    /**
     * 最新SOC（%，可选）
     */
    private BigDecimal lastSoc;

    /**
     * 最新累计电量（kWh，可选）
     */
    private BigDecimal lastEnergy;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String chargerCode;
}
