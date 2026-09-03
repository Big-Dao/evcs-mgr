package com.evcs.station.dto;

import com.evcs.station.entity.ChargerConnectorCurvePoint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 充电曲线采样点响应 DTO
 *
 * <p>审计人（createBy/updateBy）、逻辑删除与乐观锁字段属于 Entity 内部结构，不对外暴露。
 */
@Value
@Builder
public class ChargerConnectorCurvePointResponse {
    Long id;
    Long tenantId;
    Long chargerId;
    Integer connectorNo;
    String sessionId;
    LocalDateTime sampleTime;
    BigDecimal voltage;
    BigDecimal currentA;
    BigDecimal power;
    BigDecimal soc;
    BigDecimal energy;
    Long durationSeconds;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static ChargerConnectorCurvePointResponse from(com.evcs.station.entity.ChargerConnectorCurvePoint e) {
        if (e == null) {
            return null;
        }
        return ChargerConnectorCurvePointResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .chargerId(e.getChargerId())
                .connectorNo(e.getConnectorNo())
                .sessionId(e.getSessionId())
                .sampleTime(e.getSampleTime())
                .voltage(e.getVoltage())
                .currentA(e.getCurrentA())
                .power(e.getPower())
                .soc(e.getSoc())
                .energy(e.getEnergy())
                .durationSeconds(e.getDurationSeconds())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
