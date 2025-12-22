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
 * 充电枪口会话历史
 * 对应 charger_connector_session 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "charger_connector_session")
public class ChargerConnectorSession extends BaseEntity {

    @TableId(value = "charger_connector_session_id", type = IdType.AUTO)
    private Long id;

    private Long chargerId;

    /**
     * 枪口编号（从 1 开始）
     */
    private Integer connectorNo;

    /**
     * 会话ID（例如：OCPP_TXN_<transactionId>）
     */
    private String sessionId;

    private String protocolType;

    private LocalDateTime startTime;

    private LocalDateTime stopTime;

    private BigDecimal initialEnergy;

    private BigDecimal totalEnergy;

    private Long durationSeconds;

    private LocalDateTime lastSampleTime;

    private BigDecimal lastVoltage;

    private BigDecimal lastCurrent;

    private BigDecimal lastPower;

    private BigDecimal lastSoc;

    private BigDecimal lastEnergy;

    /**
     * 1=ACTIVE, 2=STOPPED
     */
    private Integer status;
}
