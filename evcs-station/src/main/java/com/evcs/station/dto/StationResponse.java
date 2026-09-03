package com.evcs.station.dto;

import com.evcs.station.entity.Station;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 充电站响应 DTO
 *
 * <p>审计人（createBy/updateBy）、逻辑删除与乐观锁字段属于 Entity 内部结构，不对外暴露。
 */
@Value
@Builder
public class StationResponse {
    Long stationId;
    Long tenantId;
    String stationCode;
    String stationName;
    String address;
    Double latitude;
    Double longitude;
    Integer status;
    String province;
    String city;
    String district;
    Integer totalChargers;
    Integer availableChargers;
    Integer chargingChargers;
    Integer faultChargers;
    LocalDateTime createTime;
    LocalDateTime updateTime;

    public static StationResponse from(com.evcs.station.entity.Station e) {
        if (e == null) {
            return null;
        }
        return StationResponse.builder()
                .stationId(e.getStationId())
                .tenantId(e.getTenantId())
                .stationCode(e.getStationCode())
                .stationName(e.getStationName())
                .address(e.getAddress())
                .latitude(e.getLatitude())
                .longitude(e.getLongitude())
                .status(e.getStatus())
                .province(e.getProvince())
                .city(e.getCity())
                .district(e.getDistrict())
                .totalChargers(e.getTotalChargers())
                .availableChargers(e.getAvailableChargers())
                .chargingChargers(e.getChargingChargers())
                .faultChargers(e.getFaultChargers())
                .createTime(e.getCreateTime())
                .updateTime(e.getUpdateTime())
                .build();
    }
}
