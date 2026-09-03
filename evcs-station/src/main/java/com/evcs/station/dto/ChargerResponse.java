package com.evcs.station.dto;

import com.evcs.station.entity.Charger;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 充电桩响应 DTO
 *
 * <p>审计人（createBy/updateBy）、逻辑删除与乐观锁字段属于 Entity 内部结构，不对外暴露。
 */
@Value
@Builder
public class ChargerResponse {
    Long id;
    Long stationId;
    Long tenantId;
    String chargerCode;
    String chargerName;
    String stationCode;
    Integer chargerType;
    String brand;
    String model;
    String manufacturer;
    LocalDate productionDate;
    LocalDate operationDate;
    BigDecimal ratedPower;
    Integer inputVoltage;
    String outputVoltageRange;
    String outputCurrentRange;
    Integer gunCount;
    String gunTypes;
    String supportedProtocols;
    Integer status;
    String faultCode;
    String faultDescription;
    LocalDateTime lastHeartbeat;
    Long totalChargingSessions;
    BigDecimal totalChargingEnergy;
    Long totalChargingTime;
    String currentSessionId;
    Long currentUserId;
    LocalDateTime chargingStartTime;
    BigDecimal chargedEnergy;
    Integer chargedDuration;
    BigDecimal currentPower;
    BigDecimal currentVoltage;
    BigDecimal currentCurrent;
    BigDecimal temperature;
    Integer signalStrength;
    String firmwareVersion;
    LocalDateTime lastMaintenanceTime;
    LocalDateTime nextMaintenanceTime;
    Integer enabled;
    Long billingPlanId;
    String remark;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static ChargerResponse from(com.evcs.station.entity.Charger e) {
        if (e == null) {
            return null;
        }
        return ChargerResponse.builder()
                .id(e.getId())
                .stationId(e.getStationId())
                .tenantId(e.getTenantId())
                .chargerCode(e.getChargerCode())
                .chargerName(e.getChargerName())
                .stationCode(e.getStationCode())
                .chargerType(e.getChargerType())
                .brand(e.getBrand())
                .model(e.getModel())
                .manufacturer(e.getManufacturer())
                .productionDate(e.getProductionDate())
                .operationDate(e.getOperationDate())
                .ratedPower(e.getRatedPower())
                .inputVoltage(e.getInputVoltage())
                .outputVoltageRange(e.getOutputVoltageRange())
                .outputCurrentRange(e.getOutputCurrentRange())
                .gunCount(e.getGunCount())
                .gunTypes(e.getGunTypes())
                .supportedProtocols(e.getSupportedProtocols())
                .status(e.getStatus())
                .faultCode(e.getFaultCode())
                .faultDescription(e.getFaultDescription())
                .lastHeartbeat(e.getLastHeartbeat())
                .totalChargingSessions(e.getTotalChargingSessions())
                .totalChargingEnergy(e.getTotalChargingEnergy())
                .totalChargingTime(e.getTotalChargingTime())
                .currentSessionId(e.getCurrentSessionId())
                .currentUserId(e.getCurrentUserId())
                .chargingStartTime(e.getChargingStartTime())
                .chargedEnergy(e.getChargedEnergy())
                .chargedDuration(e.getChargedDuration())
                .currentPower(e.getCurrentPower())
                .currentVoltage(e.getCurrentVoltage())
                .currentCurrent(e.getCurrentCurrent())
                .temperature(e.getTemperature())
                .signalStrength(e.getSignalStrength())
                .firmwareVersion(e.getFirmwareVersion())
                .lastMaintenanceTime(e.getLastMaintenanceTime())
                .nextMaintenanceTime(e.getNextMaintenanceTime())
                .enabled(e.getEnabled())
                .billingPlanId(e.getBillingPlanId())
                .remark(e.getRemark())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
