package com.evcs.protocol.dto.ocpp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * OCPP BootNotification请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OCPPBootNotificationRequest extends OCPPRequestPayload {

    /**
     * 充电站厂商
     */
    @JsonProperty("chargePointVendor")
    private String chargePointVendor;

    /**
     * 充电站型号
     */
    @JsonProperty("chargePointModel")
    private String chargePointModel;

    /**
     * 充电站序列号
     */
    @JsonProperty("chargePointSerialNumber")
    private String chargePointSerialNumber;

    /**
     * 充电站固件版本
     */
    @JsonProperty("firmwareVersion")
    private String firmwareVersion;

    /**
     * 充电站ICCID
     */
    @JsonProperty("iccid")
    private String iccid;

    /**
     * 充电站IMSI
     */
    @JsonProperty("imsi")
    private String imsi;

    /**
     * 充电站计量类型
     */
    @JsonProperty("meterType")
    private String meterType;

    /**
     * 充电站计量序列号
     */
    @JsonProperty("meterSerialNumber")
    private String meterSerialNumber;

    public OCPPBootNotificationRequest() {
        super();
    }

    public OCPPBootNotificationRequest(String chargePointVendor, String chargePointModel) {
        this();
        this.chargePointVendor = chargePointVendor;
        this.chargePointModel = chargePointModel;
    }

    /**
     * 创建BootNotification请求
     */
    public static OCPPBootNotificationRequest create(String vendor, String model) {
        OCPPBootNotificationRequest request = new OCPPBootNotificationRequest();
        request.setChargePointVendor(vendor);
        request.setChargePointModel(model);
        request.setTimestamp(LocalDateTime.now());
        return request;
    }

    @Override
    public String getAction() {
        return "BootNotification";
    }

    @Override
    public boolean isValid() {
        return chargePointVendor != null && !chargePointVendor.trim().isEmpty() &&
               chargePointModel != null && !chargePointModel.trim().isEmpty();
    }

    /**
     * 获取设备标识信息
     */
    public String getDeviceIdentifier() {
        StringBuilder sb = new StringBuilder();
        sb.append(chargePointVendor).append("_").append(chargePointModel);
        if (chargePointSerialNumber != null) {
            sb.append("_").append(chargePointSerialNumber);
        }
        return sb.toString();
    }
}

/**
 * OCPP BootNotification响应
 */
@Data
class OCPPBootNotificationResponse extends OCPPResponsePayload {

    /**
     * 当前时间
     */
    @JsonProperty("currentTime")
    private LocalDateTime currentTime;

    /**
     * 间隔时间（秒）
     */
    @JsonProperty("interval")
    private Integer interval;

    /**
     * 状态
     */
    @JsonProperty("status")
    private OCPPRegistrationStatus status;

    public OCPPBootNotificationResponse() {
        super();
        this.currentTime = LocalDateTime.now();
    }

    public OCPPBootNotificationResponse(OCPPRegistrationStatus status, Integer interval) {
        this();
        this.status = status;
        this.interval = interval;
    }

    /**
     * 创建成功响应
     */
    public static OCPPBootNotificationResponse success(Integer interval) {
        return new OCPPBootNotificationResponse(OCPPRegistrationStatus.ACCEPTED, interval);
    }

    /**
     * 创建失败响应
     */
    public static OCPPBootNotificationResponse failure(OCPPRegistrationStatus status) {
        return new OCPPBootNotificationResponse(status, null);
    }

    @Override
    public boolean isValid() {
        return status != null && interval != null && interval > 0;
    }
}

/**
 * OCPP注册状态枚举
 */
enum OCPPRegistrationStatus {
    @JsonProperty("Accepted")
    ACCEPTED,

    @JsonProperty("Pending")
    PENDING,

    @JsonProperty("Rejected")
    REJECTED
}