package com.evcs.station.dto;

import com.evcs.station.entity.ChargerConnector;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 充电枪响应 DTO
 *
 * <p>审计人（createBy/updateBy）、逻辑删除与乐观锁字段属于 Entity 内部结构，不对外暴露。
 */
@Value
@Builder
public class ChargerConnectorResponse {
    Long id;
    Long tenantId;
    Long chargerId;
    Integer connectorNo;
    String connectorType;
    Integer status;
    String faultCode;
    String faultDescription;
    LocalDateTime lastHeartbeat;
    String currentSessionId;
    Long currentUserId;
    LocalDateTime chargingStartTime;
    BigDecimal chargedEnergy;
    Integer chargedDuration;
    LocalDateTime lastMeterTime;
    BigDecimal lastVoltage;
    BigDecimal lastCurrent;
    BigDecimal lastPower;
    BigDecimal lastSoc;
    BigDecimal lastEnergy;
    String chargerCode;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static ChargerConnectorResponse from(com.evcs.station.entity.ChargerConnector e) {
        if (e == null) {
            return null;
        }
        return ChargerConnectorResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .chargerId(e.getChargerId())
                .connectorNo(e.getConnectorNo())
                .connectorType(e.getConnectorType())
                .status(e.getStatus())
                .faultCode(e.getFaultCode())
                .faultDescription(e.getFaultDescription())
                .lastHeartbeat(e.getLastHeartbeat())
                .currentSessionId(e.getCurrentSessionId())
                .currentUserId(e.getCurrentUserId())
                .chargingStartTime(e.getChargingStartTime())
                .chargedEnergy(e.getChargedEnergy())
                .chargedDuration(e.getChargedDuration())
                .lastMeterTime(e.getLastMeterTime())
                .lastVoltage(e.getLastVoltage())
                .lastCurrent(e.getLastCurrent())
                .lastPower(e.getLastPower())
                .lastSoc(e.getLastSoc())
                .lastEnergy(e.getLastEnergy())
                .chargerCode(e.getChargerCode())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
