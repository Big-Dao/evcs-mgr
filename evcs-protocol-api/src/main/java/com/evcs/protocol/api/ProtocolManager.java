package com.evcs.protocol.api;

import com.evcs.protocol.dto.ProtocolRequest;
import com.evcs.protocol.dto.ProtocolResponse;
import com.evcs.protocol.enums.ProtocolType;

import java.util.List;

/**
 * 协议管理器接口
 * 负责协议的选择、路由和管理
 */
public interface ProtocolManager {

    /**
     * 注册协议服务
     *
     * @param protocolType 协议类型
     * @param protocolService 协议服务实现
     */
    void registerProtocolService(ProtocolType protocolType, IProtocolService protocolService);

    /**
     * 获取协议服务
     *
     * @param protocolType 协议类型
     * @return 协议服务实现
     */
    IProtocolService getProtocolService(ProtocolType protocolType);

    /**
     * 根据设备编码获取协议类型
     *
     * @param deviceCode 设备编码
     * @return 协议类型
     */
    ProtocolType getProtocolType(String deviceCode);

    /**
     * 设置设备协议类型
     *
     * @param deviceCode 设备编码
     * @param protocolType 协议类型
     */
    void setDeviceProtocolType(String deviceCode, ProtocolType protocolType);

    /**
     * 处理协议请求（自动路由到对应的协议服务）
     *
     * @param request 协议请求
     * @return 协议响应
     */
    ProtocolResponse handleRequest(ProtocolRequest request);

    /**
     * 连接设备（自动选择协议）
     *
     * @param deviceCode 设备编码
     * @param protocolType 协议类型
     * @return 连接结果
     */
    boolean connect(String deviceCode, ProtocolType protocolType);

    /**
     * 断开设备连接
     *
     * @param deviceCode 设备编码
     */
    void disconnect(String deviceCode);

    /**
     * 获取所有已注册的协议类型
     *
     * @return 协议类型列表
     */
    List<ProtocolType> getRegisteredProtocolTypes();

    /**
     * 检查设备是否连接
     *
     * @param deviceCode 设备编码
     * @return 是否连接
     */
    boolean isDeviceConnected(String deviceCode);

    /**
     * 获取已连接的设备数量
     *
     * @param protocolType 协议类型
     * @return 连接数量
     */
    int getConnectedDeviceCount(ProtocolType protocolType);

    /**
     * 设置全局事件监听器
     *
     * @param listener 事件监听器
     */
    void setEventListener(ProtocolEventListener listener);
}