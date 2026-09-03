package com.evcs.station.dto;

import com.evcs.station.entity.ChargerConnectorSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 充电会话响应 DTO
 *
 * <p>审计人（createBy/updateBy）、逻辑删除与乐观锁字段属于 Entity 内部结构，不对外暴露。
 */
@Value
@Builder
public class ChargerConnectorSessionResponse {
    Long id;
    Long tenantId;
    Long chargerId;
    Integer connectorNo;
    String sessionId;
    String protocolType;
    LocalDateTime startTime;
    LocalDateTime stopTime;
    BigDecimal initialEnergy;
    BigDecimal totalEnergy;
    Long durationSeconds;
    LocalDateTime lastSampleTime;
    BigDecimal lastVoltage;
    BigDecimal lastCurrent;
    BigDecimal lastPower;
    BigDecimal lastSoc;
    BigDecimal lastEnergy;
    Integer status;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static ChargerConnectorSessionResponse from(com.evcs.station.entity.ChargerConnectorSession e) {
        if (e == null) {
            return null;
        }
        return ChargerConnectorSessionResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .chargerId(e.getChargerId())
                .connectorNo(e.getConnectorNo())
                .sessionId(e.getSessionId())
                .protocolType(e.getProtocolType())
                .startTime(e.getStartTime())
                .stopTime(e.getStopTime())
                .initialEnergy(e.getInitialEnergy())
                .totalEnergy(e.getTotalEnergy())
                .durationSeconds(e.getDurationSeconds())
                .lastSampleTime(e.getLastSampleTime())
                .lastVoltage(e.getLastVoltage())
                .lastCurrent(e.getLastCurrent())
                .lastPower(e.getLastPower())
                .lastSoc(e.getLastSoc())
                .lastEnergy(e.getLastEnergy())
                .status(e.getStatus())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
