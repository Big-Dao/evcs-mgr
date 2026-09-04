package com.evcs.station.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 输入请求 DTO：仅暴露调用方可写业务字段。
 * 租户归属、逻辑删除、审计人与设备运行时字段不在绑定面（批量赋值防护）。
 */
@Data
public class ChargerUpsertRequest {
    /** 定位主键（更新时必填） */
    private Long id;
    private String chargerCode;
    private String chargerName;
    private Long stationId;
    private String stationCode;
    private Integer chargerType;
    private String brand;
    private String model;
    private String manufacturer;
    private LocalDate productionDate;
    private LocalDate operationDate;
    private BigDecimal ratedPower;
    private Integer inputVoltage;
    private String outputVoltageRange;
    private String outputCurrentRange;
    private Integer gunCount;
    private String gunTypes;
    private String supportedProtocols;
    private Integer enabled;
    private String firmwareVersion;
    private Long billingPlanId;
    private String remark;

    public com.evcs.station.entity.Charger toEntity() {
        com.evcs.station.entity.Charger e = new com.evcs.station.entity.Charger();
        applyTo(e);
        return e;
    }

    public void applyTo(com.evcs.station.entity.Charger e) {
        e.setId(id);
        e.setChargerCode(chargerCode);
        e.setChargerName(chargerName);
        e.setStationId(stationId);
        e.setStationCode(stationCode);
        e.setChargerType(chargerType);
        e.setBrand(brand);
        e.setModel(model);
        e.setManufacturer(manufacturer);
        e.setProductionDate(productionDate);
        e.setOperationDate(operationDate);
        e.setRatedPower(ratedPower);
        e.setInputVoltage(inputVoltage);
        e.setOutputVoltageRange(outputVoltageRange);
        e.setOutputCurrentRange(outputCurrentRange);
        e.setGunCount(gunCount);
        e.setGunTypes(gunTypes);
        e.setSupportedProtocols(supportedProtocols);
        e.setEnabled(enabled);
        e.setFirmwareVersion(firmwareVersion);
        e.setBillingPlanId(billingPlanId);
        e.setRemark(remark);
    }
}
