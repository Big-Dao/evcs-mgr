package com.evcs.station.protocol;

import com.evcs.station.entity.Charger;

/**
 * OCPP协议接口
 * 为充电桩提供OCPP 1.6/2.0.1协议支持的扩展点
 */
public interface IOCPPProtocolService {

    /**
     * 处理充电桩心跳消息
     */
    boolean handleHeartbeat(String chargerCode, String payload);

    /**
     * 处理充电桩状态通知
     */
    boolean handleStatusNotification(String chargerCode, Integer connectorId, String status, String errorCode);

    /**
     * 处理充电桩启动通知
     */
    boolean handleBootNotification(String chargerCode, String vendor, String model, String serialNumber);

    /**
     * 发送远程启动充电命令
     */
    boolean remoteStartTransaction(String chargerCode, Integer connectorId, String idTag);

    /**
     * 发送远程停止充电命令
     */
    boolean remoteStopTransaction(String chargerCode, Integer transactionId);

    /**
     * 发送充电桩重置命令
     */
    boolean reset(String chargerCode, String type);

    /**
     * 发送解锁连接器命令
     */
    boolean unlockConnector(String chargerCode, Integer connectorId);

    /**
     * 获取充电桩配置
     */
    String getConfiguration(String chargerCode, String[] keys);

    /**
     * 设置充电桩配置
     */
    boolean changeConfiguration(String chargerCode, String key, String value);

    /**
     * 处理充电开始事务
     */
    boolean handleStartTransaction(String chargerCode, Integer connectorId, String idTag, Integer meterStart, String timestamp);

    /**
     * 处理充电停止事务
     */
    boolean handleStopTransaction(String chargerCode, Integer transactionId, String idTag, Integer meterStop, String timestamp);

    /**
     * 处理计量值报告
     */
    boolean handleMeterValues(String chargerCode, Integer connectorId, Integer transactionId, String values, String timestamp);

    /**
     * 验证充电桩是否支持OCPP协议
     */
    boolean isOCPPSupported(Charger charger);

    /**
     * 获取支持的OCPP版本
     */
    String getSupportedOCPPVersion(Charger charger);
}