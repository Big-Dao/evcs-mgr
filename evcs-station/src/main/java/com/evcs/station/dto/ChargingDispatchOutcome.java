package com.evcs.station.dto;

/**
 * 充电指令下发结果（服务层编排的聚合结论）。
 */
public record ChargingDispatchOutcome(boolean success, String message) {

    public static ChargingDispatchOutcome success(String message) {
        return new ChargingDispatchOutcome(true, message);
    }

    public static ChargingDispatchOutcome fail(String message) {
        return new ChargingDispatchOutcome(false, message);
    }
}
