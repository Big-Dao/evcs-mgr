package com.evcs.station.dto;

import lombok.Data;

/**
 * 输入请求 DTO：仅暴露调用方可写业务字段。
 * 租户归属、逻辑删除、审计人与设备运行时字段不在绑定面（批量赋值防护）。
 */
@Data
public class StationUpsertRequest {
    private String stationCode;
    private String stationName;
    private String address;
    private Double latitude;
    private Double longitude;
    private Integer status; // 1-启用 0-停用
    private String province;
    private String city;
    private String district;

    public com.evcs.station.entity.Station toEntity() {
        com.evcs.station.entity.Station e = new com.evcs.station.entity.Station();
        e.setStationCode(stationCode);
        e.setStationName(stationName);
        e.setAddress(address);
        e.setLatitude(latitude);
        e.setLongitude(longitude);
        e.setStatus(status);
        e.setProvince(province);
        e.setCity(city);
        e.setDistrict(district);
        return e;
    }

    public void applyTo(com.evcs.station.entity.Station e) {
        e.setStationName(stationName);
        e.setAddress(address);
        e.setLatitude(latitude);
        e.setLongitude(longitude);
        e.setStatus(status);
        e.setProvince(province);
        e.setCity(city);
        e.setDistrict(district);
    }
}
