package com.evcs.order.dto;


import lombok.Data;

/**
 * 输入请求 DTO：仅暴露调用方可写业务字段。
 * 租户归属、逻辑删除、审计人与设备运行时字段不在绑定面（批量赋值防护）。
 */
@Data
public class BillingRateUpsertRequest {
    private Long stationId;              // null 表示租户默认
    private Integer touEnabled;          // 0/1
    private String peakStart;            // HH:mm
    private String peakEnd;              // HH:mm
    private java.math.BigDecimal peakPrice;
    private java.math.BigDecimal offpeakPrice;
    private java.math.BigDecimal flatPrice;
    private java.math.BigDecimal serviceFee;
    private Integer status;              // 1-启用 0-禁用

    public com.evcs.order.entity.BillingRate toEntity() {
        com.evcs.order.entity.BillingRate e = new com.evcs.order.entity.BillingRate();
        e.setStationId(stationId);
        e.setTouEnabled(touEnabled);
        e.setPeakStart(peakStart);
        e.setPeakEnd(peakEnd);
        e.setPeakPrice(peakPrice);
        e.setOffpeakPrice(offpeakPrice);
        e.setFlatPrice(flatPrice);
        e.setServiceFee(serviceFee);
        e.setStatus(status);
        return e;
    }

    public void applyTo(com.evcs.order.entity.BillingRate e) {
        e.setStationId(stationId);
        e.setTouEnabled(touEnabled);
        e.setPeakStart(peakStart);
        e.setPeakEnd(peakEnd);
        e.setPeakPrice(peakPrice);
        e.setOffpeakPrice(offpeakPrice);
        e.setFlatPrice(flatPrice);
        e.setServiceFee(serviceFee);
        e.setStatus(status);
    }
}
