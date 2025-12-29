package com.evcs.station.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 充电桩状态枚举
 */
@Getter
@AllArgsConstructor
public enum ChargerStatus {
    OFFLINE(0, "离线"),
    IDLE(1, "空闲"),
    CHARGING(2, "充电中"),
    FAULT(3, "故障"),
    MAINTAIN(4, "维护中"),
    BOOKED(5, "预约中");

    private final int code;
    private final String description;

    public static ChargerStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(status -> status.getCode() == code)
                .findFirst()
                .orElse(null);
    }
}
