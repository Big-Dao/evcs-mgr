package com.evcs.station.protocol;

import com.evcs.station.entity.Charger;

/**
 * 云快充协议接口
 * 为充电桩提供云快充协议支持的扩展点
 */
public interface ICloudChargeProtocolService {

    /**
     * 处理充电桩注册
     */
    boolean handleDeviceRegister(String chargerCode, String deviceInfo);

    /**
     * 处理充电桩状态上报
     */
    boolean handleStatusReport(String chargerCode, String status, String data);

    /**
     * 发送充电启动命令
     */
    boolean startCharge(String chargerCode, String orderNo, String userInfo);

    /**
     * 发送充电停止命令
     */
    boolean stopCharge(String chargerCode, String orderNo);

    /**
     * 查询充电桩状态
     */
    String queryChargerStatus(String chargerCode);

    /**
     * 设置充电参数
     */
    boolean setChargeParams(String chargerCode, String params);

    /**
     * 处理充电数据上报
     */
    boolean handleChargeDataReport(String chargerCode, String orderNo, String chargeData);

    /**
     * 处理故障报告
     */
    boolean handleFaultReport(String chargerCode, String faultCode, String faultDesc);

    /**
     * 远程升级充电桩固件
     */
    boolean remoteFirmwareUpdate(String chargerCode, String firmwareUrl, String version);

    /**
     * 远程重启充电桩
     */
    boolean remoteRestart(String chargerCode);

    /**
     * 验证充电桩是否支持云快充协议
     */
    boolean isCloudChargeSupported(Charger charger);

    /**
     * 获取支持的云快充协议版本
     */
    String getSupportedCloudChargeVersion(Charger charger);
}