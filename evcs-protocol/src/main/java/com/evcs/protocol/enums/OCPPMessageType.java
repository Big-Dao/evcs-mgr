package com.evcs.protocol.enums;

/**
 * OCPP消息类型枚举 (基于OCPP 1.6规范)
 */
public enum OCPPMessageType {

    // 核心消息类型
    CALL(2, "Call"),
    CALL_RESULT(3, "CallResult"),
    CALL_ERROR(4, "CallError"),

    // 充电站管理消息
    BOOT_NOTIFICATION("BootNotification"),
    HEARTBEAT("Heartbeat"),
    STATUS_NOTIFICATION("StatusNotification"),
    FIRMWARE_UPDATE_NOTIFICATION("FirmwareUpdateNotification"),
    DIAGNOSTICS_STATUS_NOTIFICATION("DiagnosticsStatusNotification"),

    // 授权相关消息
    AUTHORIZE("Authorize"),
    START_TRANSACTION("StartTransaction"),
    STOP_TRANSACTION("StopTransaction"),
    DATA_TRANSFER("DataTransfer"),

    // 远程控制消息
    REMOTE_START_TRANSACTION("RemoteStartTransaction"),
    REMOTE_STOP_TRANSACTION("RemoteStopTransaction"),
    UNLOCK_CONNECTOR("UnlockConnector"),
    RESET("Reset"),
    CLEAR_CACHE("ClearCache"),
    CHANGE_AVAILABILITY("ChangeAvailability"),
    CHANGE_CONFIGURATION("ChangeConfiguration"),
    GET_CONFIGURATION("GetConfiguration"),
    SET_CHARGING_PROFILE("SetChargingProfile"),
    CLEAR_CHARGING_PROFILE("ClearChargingProfile"),
    GET_COMPOSITE_SCHEDULE("GetCompositeSchedule"),
    GET_LOCAL_LIST_VERSION("GetLocalListVersion"),
    SEND_LOCAL_LIST("SendLocalList"),

    // 智能充电消息

    // 诊断消息
    TRIGGER_MESSAGE("TriggerMessage"),
    GET_DIAGNOSTICS("GetDiagnostics"),
    UPDATE_FIRMWARE("UpdateFirmware"),

    // 测量值消息
    METER_VALUES("MeterValues"),

    // 预留消息（用于OCPP 2.0兼容性）
    RESERVE_NOW("ReserveNow"),
    CANCEL_RESERVATION("CancelReservation");

    private final Integer typeId;
    private final String action;

    OCPPMessageType(Integer typeId, String action) {
        this.typeId = typeId;
        this.action = action;
    }

    OCPPMessageType(String action) {
        this.typeId = null;
        this.action = action;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public String getAction() {
        return action;
    }

    /**
     * 根据类型ID获取消息类型
     */
    public static OCPPMessageType fromTypeId(Integer typeId) {
        for (OCPPMessageType type : values()) {
            if (type.typeId != null && type.typeId.equals(typeId)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown OCPP message type ID: " + typeId);
    }

    /**
     * 根据动作名称获取消息类型
     */
    public static OCPPMessageType fromAction(String action) {
        for (OCPPMessageType type : values()) {
            if (type.action.equals(action)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown OCPP message action: " + action);
    }

    /**
     * 检查是否为Call消息
     */
    public boolean isCall() {
        return CALL.equals(this);
    }

    /**
     * 检查是否为CallResult消息
     */
    public boolean isCallResult() {
        return CALL_RESULT.equals(this);
    }

    /**
     * 检查是否为CallError消息
     */
    public boolean isCallError() {
        return CALL_ERROR.equals(this);
    }

    /**
     * 检查是否为核心消息类型（2/3/4）
     */
    public boolean isCoreMessageType() {
        return typeId != null && typeId >= 2 && typeId <= 4;
    }

    /**
     * 检查是否为动作消息
     */
    public boolean isActionMessage() {
        return typeId == null;
    }

    @Override
    public String toString() {
        return action + (typeId != null ? " (" + typeId + ")" : "");
    }
}